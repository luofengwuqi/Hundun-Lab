package com.hundunlab.site.module.admin.service;

import com.hundunlab.site.module.user.pojo.DTO.GetLogsDTO;
import com.hundunlab.site.module.admin.pojo.DTO.AddAdminDTO;
import com.hundunlab.site.module.admin.pojo.DTO.GetUsersDTO;
import com.hundunlab.site.module.user.pojo.DTO.UpdateUserDTO;
import com.hundunlab.site.module.user.pojo.Entity.OperateLog;
import com.hundunlab.site.module.user.pojo.Entity.User;
import com.hundunlab.site.module.user.pojo.VO.PageVO;
import jakarta.validation.Valid;

public interface AdminService {



    Integer getUserNumber();


    Integer changeStatus(String username);


    void addAdmin(AddAdminDTO dto);

    PageVO<User> getUsers(GetUsersDTO dto);

    String makeInviteCode();

//    void deleteUserByUserName(String username);

    PageVO<OperateLog> getLogs(@Valid GetLogsDTO dto);

    void deleteUserById(Long id);

    void updateUser(UpdateUserDTO dto);

    User getUserInfoById(Long id);
}
