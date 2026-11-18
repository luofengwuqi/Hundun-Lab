package com.hundunlab.site.module.admin.service;

import com.hundunlab.site.module.user.pojo.DTO.GetLogsDTO;
import com.hundunlab.site.module.admin.pojo.DTO.AddAdminDTO;
import com.hundunlab.site.module.admin.pojo.DTO.GetUsersDTO;
import com.hundunlab.site.module.admin.pojo.VO.InviteCodeVO;
import com.hundunlab.site.module.user.pojo.DTO.UpdateUserDTO;
import com.hundunlab.site.module.user.pojo.Entity.OperateLog;
import com.hundunlab.site.module.user.pojo.Entity.User;
import com.hundunlab.site.module.user.pojo.VO.PageVO;
import jakarta.validation.Valid;

import java.util.List;

public interface AdminService {



    Integer getUserNumber();


    Integer changeStatus(String username);


    void addAdmin(AddAdminDTO dto);

    PageVO<User> getUsers(GetUsersDTO dto);

    String makeInviteCode();

    /** 待用邀请码列表（超管专属，码本身即凭证，不做缓存免得撤销后还挂着） */
    List<InviteCodeVO> listInvites();

//    void deleteUserByUserName(String username);

    PageVO<OperateLog> getLogs(@Valid GetLogsDTO dto);

    void deleteUserById(Long id);

    void updateUser(UpdateUserDTO dto);

    User getUserInfoById(Long id);
}
