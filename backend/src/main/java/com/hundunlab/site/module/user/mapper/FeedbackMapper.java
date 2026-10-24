package com.hundunlab.site.module.user.mapper;

import com.hundunlab.site.common.anno.AutoLog;
import com.hundunlab.site.common.constant.ModuleNameConstant;
import com.hundunlab.site.common.constant.type.OperateTypeConstant;
import com.hundunlab.site.module.user.pojo.Entity.Feedback;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface FeedbackMapper {
    @Select("select * from feedback")
    List<Feedback> selectAllFeedback();
    
    /**
     * 更新反馈状态
     *
     * @param id           反馈ID
     * @param status       新状态
     * @return 影响的行数
     */
    @AutoLog(operateType = OperateTypeConstant.UPDATE, moduleName = ModuleNameConstant.FEEDBACK)
    @Update("update feedback set status = #{status},finished_at = #{now},handle_user_id=#{handleUserId} where id = #{id}")
    int updateFeedbackStatus(Integer id, Integer status, LocalDateTime now, Integer handleUserId);

    @Insert("insert into feedback (type,text,user_id,username,user_email,status,created_at) values (#{type},#{text},#{userId},#{username},#{userEmail},#{status},#{createdAt})")
    void insertFeedback(Feedback feedback);

    @Select("select * from feedback where id = #{id}")
    Feedback selectFeedbackById(Integer id);

    /**
     * 批量删除反馈
     *
     * @param ids 反馈ID列表
     */
    void deleteFeedbackByIds(List<Integer> ids);
}
