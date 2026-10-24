package com.hundunlab.site.module.auth.service;

import com.hundunlab.site.module.auth.pojo.dto.LoginEmailCodeDTO;
import com.hundunlab.site.module.auth.pojo.dto.LoginEmailPassWordDTO;
import com.hundunlab.site.module.auth.pojo.dto.LoginPhonePasswordDTO;
import com.hundunlab.site.module.auth.pojo.dto.LoginUsernamePasswordRequestDTO;
import com.hundunlab.site.module.user.pojo.VO.LoginRequestVO;
import jakarta.validation.Valid;

public interface LoginService {
    LoginRequestVO loginPhonePassword(LoginPhonePasswordDTO dto);

    LoginRequestVO loginUsernamePassword(@Valid LoginUsernamePasswordRequestDTO l);

    LoginRequestVO loginEmailCode(LoginEmailCodeDTO dto);

    void sendEmailCode(String email);

    LoginRequestVO loginEmailPassword(LoginEmailPassWordDTO dto);

    void logout();
}
