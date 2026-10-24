package com.hundunlab.site.module.user.service;

import com.hundunlab.site.module.user.pojo.DTO.AddFeedbackDTO;
import com.hundunlab.site.module.user.pojo.DTO.UpdateFeedbackStatusDTO;
import com.hundunlab.site.module.user.pojo.Entity.Feedback;
import jakarta.validation.Valid;

import java.util.List;

public interface FeedbackService {
    List<Feedback> selectAllFeedback(int page, int size);
    
    /**
     * 更新反馈状态
     * @param dto 包含反馈ID和新状态的DTO
     */
    void updateFeedbackStatus(UpdateFeedbackStatusDTO dto);

    void addFeedback(@Valid AddFeedbackDTO dto);

    void deleteHandledFeedback();
}
