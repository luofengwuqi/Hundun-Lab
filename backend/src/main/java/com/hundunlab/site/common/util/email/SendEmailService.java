package com.hundunlab.site.common.util.email;

import com.hundunlab.site.common.exception.MyException.UserNotFoundException;
import com.hundunlab.site.module.user.cache.UserRedisService;
import com.hundunlab.site.module.user.mapper.UserMapper;
import com.hundunlab.site.module.admin.pojo.DTO.GetUsersDTO;
import com.hundunlab.site.module.user.pojo.Entity.User;
import com.hundunlab.site.module.admin.service.AdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.hundunlab.site.common.constant.CommonConstant.LARGE_NUMBER;
import static com.hundunlab.site.common.constant.status.UserStatusConstant.STATUS_ADMIN;
import static com.hundunlab.site.common.constant.status.UserStatusConstant.STATUS_SUPER_ADMIN;
import static com.hundunlab.site.common.constant.email.TEXT.*;

@Slf4j
@Component
public class SendEmailService {
    private final AdminService adminService;
    private final UserMapper userMapper;
    private final UserRedisService userRedisService;
    private final SendMsgUtil sendMsgUtil;

    public SendEmailService(AdminService adminService, UserMapper userMapper, UserRedisService userRedisService, SendMsgUtil sendMsgUtil) {
        this.adminService = adminService;
        this.userMapper = userMapper;
        this.userRedisService = userRedisService;
        this.sendMsgUtil = sendMsgUtil;
    }
    @Async("emailExecutor")
    public CompletableFuture<Boolean> sendFeedbackFinishedMsgToUser(Long userId) {
        String feedbackMakerEmail = getEmail(userId);
        boolean success = sendMsgUtil.sendSingleEmail(feedbackMakerEmail, FEEDBACK_FINISHED_MSG_TO_USER);
        if (success) {
            log.info("反馈完成通知发送完成，userId={}", userId);
        } else {
            log.error("反馈完成通知发送失败，userId={}", userId);
        }
        return CompletableFuture.completedFuture(success);
    }
    @Async("emailExecutor")
    public CompletableFuture<Boolean> sendFeedbackBanedMsgToUser(Long userId) {
        String feedbackMakerEmail = getEmail(userId);
        boolean success = sendMsgUtil.sendSingleEmail(feedbackMakerEmail, FEEDBACK_BANED_MSG_TO_USER);
        if (success) {
            log.info("反馈否决通知发送完成，userId={}", userId);
        } else {
            log.error("反馈否决通知发送失败，userId={}", userId);
        }
        return CompletableFuture.completedFuture(success);
    }
    @Async("emailExecutor")
    public CompletableFuture<Boolean> sendFeedbackAddedMsgToAdmin() {
        ArrayList<String> emailList = new ArrayList<>();
        try {
            GetUsersDTO dto1 = new GetUsersDTO(1,LARGE_NUMBER,null, STATUS_ADMIN , null, null, null);
            GetUsersDTO dto2 = new GetUsersDTO(1,LARGE_NUMBER,null, STATUS_SUPER_ADMIN, null, null, null);
            
            // 查询普通管理员，如果没有则返回空列表
            //todo:管理员可以选择是否接受反馈邮件 如果不接收 那么就从集合中移除(需要一个设置表)
            List<User> adminList;
            try {
                adminList = adminService.getUsers(dto1).getList();
            } catch (Exception e) {
                log.warn("未查询到普通管理员：{}", e.getMessage());
                adminList = null;
            }
            
            // 查询超级管理员，如果没有则返回空列表
            List<User> superAdminList;
            try {
                superAdminList = adminService.getUsers(dto2).getList();
            } catch (Exception e) {
                log.warn("未查询到超级管理员：{}", e.getMessage());
                superAdminList = null;
            }
            
            // 合并两个列表
            if (adminList != null) {
                for(User user : adminList){
                    // 防止邮箱为空
                    if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                        emailList.add(user.getEmail());
                    }
                }
            }
            
            if (superAdminList != null) {
                for(User user : superAdminList){
                    // 防止邮箱为空
                    if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                        emailList.add(user.getEmail());
                    }
                }
            }
            
            if (emailList.isEmpty()) {
                log.warn("没有找到任何管理员邮箱，无法发送反馈添加通知");
                return CompletableFuture.completedFuture(false);
            }
            int successCount = sendMsgUtil.sendEmailToEmailList(emailList, FEEDBACK_ADDED_MSG_TO_ADMIN);
            log.info("反馈添加通知发送完成，目标管理员数量：{}，成功发送：{}", emailList.size(), successCount);
            return CompletableFuture.completedFuture(successCount > 0);
        } catch (Exception e) {
            log.error("发送反馈添加通知给管理员时发生异常", e);
            return CompletableFuture.completedFuture(false);
        }
    }

    private String getEmail(Long userId){
        //检查用户是否存在
        User feedbackMaker = userRedisService.getUserInRedis(userId);
        if (feedbackMaker == null){
            log.info("user:data:id缓存未命中,开始查询数据库");
            feedbackMaker = userMapper.selectUserById(userId);
        }
        if(feedbackMaker == null){
            throw new UserNotFoundException("用户不存在");
        }else{
            //存在 放入缓存
            userRedisService.saveUserInRedis(feedbackMaker);
        }
        return feedbackMaker.getEmail();
    }
}
