package com.hundunlab.site.module.user.service.serviceimpl;

import com.hundunlab.site.common.anno.RequireRole;
import com.hundunlab.site.common.context.BaseContext;
import com.hundunlab.site.common.exception.MyException.InsufficientPermissionsException;
import com.hundunlab.site.module.user.cache.FeedbackRedisService;
import com.hundunlab.site.module.user.cache.UserRedisService;
import com.hundunlab.site.common.util.email.SendEmailService;
import com.hundunlab.site.module.user.mapper.FeedbackMapper;
import com.hundunlab.site.module.user.mapper.UserMapper;
import com.hundunlab.site.module.user.pojo.DTO.AddFeedbackDTO;
import com.hundunlab.site.module.user.pojo.DTO.UpdateFeedbackStatusDTO;
import com.hundunlab.site.module.user.pojo.Entity.Feedback;
import com.hundunlab.site.module.user.pojo.Entity.User;
import com.hundunlab.site.module.user.service.FeedbackService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.hundunlab.site.common.constant.redis.RedisConstant.*;
import static com.hundunlab.site.common.constant.status.FeedbackStatusConstant.*;
import static com.hundunlab.site.common.constant.status.UserStatusConstant.STATUS_ADMIN;
import static com.hundunlab.site.common.constant.status.UserStatusConstant.STATUS_SUPER_ADMIN;

import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FeedbackServiceImpl implements FeedbackService {

    private final SendEmailService sendEmailService;
    private final UserRedisService userRedisService;
    private final FeedbackRedisService feedbackRedisService;
    private final RedisTemplate<String,Object> redisTemplate;
    private final FeedbackMapper feedbackMapper;
    private final UserMapper userMapper;

    public FeedbackServiceImpl(UserMapper userMapper, FeedbackMapper feedbackMapper, RedisTemplate<String, Object> redisTemplate, UserRedisService userRedisService, FeedbackRedisService feedbackRedisService, SendEmailService sendEmailService) {
        this.userMapper = userMapper;
        this.feedbackMapper = feedbackMapper;
        this.redisTemplate = redisTemplate;
        this.userRedisService = userRedisService;
        this.feedbackRedisService = feedbackRedisService;
        this.sendEmailService = sendEmailService;
    }

    //权限统一收敛到 @RequireRole 切面：直接查库校验，不再走 user:data 缓存（旧方案最长30分钟权限时差）
    @Override
    @RequireRole({STATUS_ADMIN, STATUS_SUPER_ADMIN})
    public List<Feedback> selectAllFeedback(int page, int size) {
        //1 分页查询，避免一次性加载全部反馈导致内存溢出
        PageHelper.startPage(page, size);
        List<Feedback> feedbacks = feedbackMapper.selectAllFeedback();
        log.info("查询反馈数据，page={}, size={}, 返回{}条", page, size, feedbacks.size());
        return feedbacks;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @RequireRole({STATUS_ADMIN, STATUS_SUPER_ADMIN})
    public void updateFeedbackStatus(UpdateFeedbackStatusDTO dto) {
        // 1. 先校验状态合法性，非法状态不落库
        boolean finished = Objects.equals(dto.getStatus(), FEEDBACK_STATUS_FINISH);
        boolean baned = Objects.equals(dto.getStatus(), FEEDBACK_STATUS_BAN);
        if (!finished && !baned) {
            throw new IllegalArgumentException("反馈状态错误");
        }

        // 2. 更新反馈状态（处理人以登录态为准，不信任客户端传参，防止审计字段伪造）
        Integer handleUserId = Long.valueOf(BaseContext.getCurrentId()).intValue();
        int rows = feedbackMapper.updateFeedbackStatus(dto.getId(), dto.getStatus(), LocalDateTime.now(), handleUserId);

        if(rows == 0){
            throw new RuntimeException("反馈不存在或更新失败");
        }

        log.info("成功更新反馈状态，反馈ID：{}，新状态：{}", dto.getId(), dto.getStatus());
        //这一步是拿到创建反馈的用户ID
        Feedback feedback = feedbackMapper.selectFeedbackById(dto.getId());
        // 3. 清除缓存，确保下次查询获取最新数据
        redisTemplate.delete(FEEDBACK_CACHE_PATH);
        // 4. 发送邮件给用户
        if(finished){
            log.info("将发送反馈完成通知邮件给用户，反馈ID：{}", dto.getId());
            logEmailResult(sendEmailService.sendFeedbackFinishedMsgToUser(Long.valueOf(feedback.getUserId())), "反馈完成通知");
        } else {
            log.info("将发送反馈驳回通知邮件给用户，反馈ID：{}", dto.getId());
            logEmailResult(sendEmailService.sendFeedbackBanedMsgToUser(Long.valueOf(feedback.getUserId())), "反馈驳回通知");
        }
        log.info("已清除反馈缓存，确保数据一致性");
    }

    @Override
    public void addFeedback(AddFeedbackDTO dto) {
        //1 dto->entity
        Feedback feedback = new Feedback();
        feedback.setType(dto.getType());
        feedback.setText(dto.getText());
        feedback.setCreatedAt(LocalDateTime.now());
        Long currentId = BaseContext.getCurrentId();
        User user= userRedisService.getUserInRedis(currentId);
        if(user == null){
            user = userMapper.selectUserById(currentId);
            userRedisService.saveUserInRedis(user);
        }
        feedback.setUserId(Math.toIntExact(user.getId()));
        feedback.setUsername(user.getUsername());
        feedback.setUserEmail(user.getEmail());
        feedback.setStatus(FEEDBACK_STATUS_UNDO);
        //2 insert
        feedbackMapper.insertFeedback(feedback);
        //3 移除feedbacks大缓存
        feedbackRedisService.clearFeedbacksCache();
        // 4 新开线程通知管理员处理
        logEmailResult(sendEmailService.sendFeedbackAddedMsgToAdmin(), "反馈新增通知");

    }

    @Override
    @RequireRole({STATUS_ADMIN, STATUS_SUPER_ADMIN})
    public void deleteHandledFeedback() {
        //查询已处理的反馈id 收集到列表
        List<Integer> handledIds = feedbackMapper.selectAllFeedback().stream()
            .filter(feedback -> Objects.equals(feedback.getStatus(), FEEDBACK_STATUS_FINISH ) || Objects.equals(feedback.getStatus(), FEEDBACK_STATUS_BAN))
            .map(Feedback::getId)
            .collect(Collectors.toList());

        //批量删除(如果有数据的话)
        if (!handledIds.isEmpty()) {
            feedbackMapper.deleteFeedbackByIds(handledIds);
            log.info("批量删除已处理反馈，数量: {}", handledIds.size());
        } else {
            log.info("没有需要删除的已处理反馈");
        }

        //缓存移除
        feedbackRedisService.clearFeedbacksCache();
    }

    /**
     * 异步邮件结果可观测：失败时记录错误日志，避免静默丢失
     */
    private void logEmailResult(CompletableFuture<Boolean> future, String scene) {
        future.whenComplete((success, ex) -> {
            if (ex != null) {
                log.error("{}发送异常", scene, ex);
            } else if (!Boolean.TRUE.equals(success)) {
                log.error("{}发送失败", scene);
            }
        });
    }

}
