package com.hundunlab.site.module.user.controller;

import com.hundunlab.site.common.anno.RequireRole;
import com.hundunlab.site.common.response.Result;
import com.hundunlab.site.module.user.pojo.DTO.AddFeedbackDTO;
import com.hundunlab.site.module.user.pojo.DTO.UpdateFeedbackStatusDTO;
import com.hundunlab.site.module.user.pojo.Entity.Feedback;
import com.hundunlab.site.module.user.service.FeedbackService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.hundunlab.site.common.constant.status.UserStatusConstant.STATUS_ADMIN;
import static com.hundunlab.site.common.constant.status.UserStatusConstant.STATUS_SUPER_ADMIN;

@Slf4j
@RestController
@RequestMapping("/feedback")
@Validated
public class FeedbackController {
    private final FeedbackService feedbackService;

    @Autowired
    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }


    //获取所有的反馈（管理员）
    @RequireRole({STATUS_ADMIN, STATUS_SUPER_ADMIN})
    @GetMapping
    public Result<List<Feedback>> selectAllFeedback(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size){
        log.info("分页获取反馈，page={}, size={}", page, size);
        return Result.success(feedbackService.selectAllFeedback(page, size));
    }
    
    //更新反馈状态（管理员）
    @RequireRole({STATUS_ADMIN, STATUS_SUPER_ADMIN})
    @PutMapping("/status")
    public Result<Void> updateFeedbackStatus(@Valid @RequestBody UpdateFeedbackStatusDTO dto){
        log.info("更新反馈状态，反馈ID：{}，新状态：{},处理人:{}", dto.getId(), dto.getStatus(),dto.getHandleUserId());
        feedbackService.updateFeedbackStatus(dto);
        return Result.success();
    }

    @PostMapping
    public Result<Void> addFeedback(@Valid @RequestBody AddFeedbackDTO dto){
        log.info("添加反馈，反馈内容：{}", dto);
        feedbackService.addFeedback(dto);
        return Result.success();
    }
    //删除已处理的反馈（管理员）
    @RequireRole({STATUS_ADMIN, STATUS_SUPER_ADMIN})
    @DeleteMapping("/handled")
    public Result<Void> deleteHandledFeedback(){
        log.info("删除已处理的反馈");
        feedbackService.deleteHandledFeedback();
        return Result.success();
    }
}
