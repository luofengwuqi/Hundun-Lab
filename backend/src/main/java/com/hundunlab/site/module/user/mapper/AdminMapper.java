package com.hundunlab.site.module.user.mapper;

import com.hundunlab.site.common.anno.AutoLog;
import com.hundunlab.site.common.constant.ModuleNameConstant;
import com.hundunlab.site.common.constant.type.OperateTypeConstant;
import com.hundunlab.site.module.admin.pojo.VO.InviteCodeVO;
import com.hundunlab.site.module.user.pojo.DTO.GetLogsDTO;
import com.hundunlab.site.module.user.pojo.DTO.UpdateUserDTO;
import com.hundunlab.site.module.user.pojo.Entity.OperateLog;
import com.hundunlab.site.module.user.pojo.Entity.User;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AdminMapper {

    @AutoLog(operateType = OperateTypeConstant.SELECT, moduleName = ModuleNameConstant.ADMIN)
    @Select("select count(*) from users")
    Integer getUserNumber();

    @AutoLog(operateType = OperateTypeConstant.SELECT, moduleName = ModuleNameConstant.ADMIN)
    List<User> getUsers(String name, Integer status, LocalDateTime beginTime, LocalDateTime endTime,Integer loginDays);

    //已被弃用
//    @AutoLog(operateType = OperateTypeConstant.DELETE, moduleName = ModuleNameConstant.ADMIN)
//    @Delete("delete from users where username = #{username}")
//    void deleteUserByUserName(String username);

    @Select("select invite_code from users where invite_code = #{importantPassword}")
    String selectInvitePassword(String importantPassword);

    /**
     * 原子消费一枚邀请码：删除它对应的占位管理员行，返回影响行数。
     * 1 = 这枚码归本次注册；0 = 不存在或刚被并发请求用掉。
     * status 条件锁死 -2（占位态）：真管理员的 invite_code 恒为 NULL，不会误删。
     */
    @AutoLog(operateType = OperateTypeConstant.DELETE, moduleName = ModuleNameConstant.ADMIN)
    @Delete("delete from users where invite_code = #{inviteCode} and status = -2")
    int deleteInvitePlaceholder(String inviteCode);

    /**
     * 待用邀请码列表（邀请管理页）：占位行即「已生成未使用」的码。
     * 只选三列——码属敏感值，别把整行 User 带出去。
     * 新码在前：超管刚生成的那枚应该出现在第一行。
     */
    @AutoLog(operateType = OperateTypeConstant.SELECT, moduleName = ModuleNameConstant.ADMIN)
    @Select("select id, invite_code as code, created_at as createdAt from users "
            + "where status = -2 and invite_code is not null "
            + "order by created_at desc, id desc")
    List<InviteCodeVO> listPendingInvites();

    //我也不知道这里适不适合加自动填充注解(我觉得还是算了
    @AutoLog(operateType = OperateTypeConstant.SELECT, moduleName = ModuleNameConstant.ADMIN)
    List<OperateLog> getLogs(GetLogsDTO dto);

    @AutoLog(operateType = OperateTypeConstant.UPDATE, moduleName = ModuleNameConstant.ADMIN)
    void updateUserByIdForAdmin(UpdateUserDTO dto);
}
