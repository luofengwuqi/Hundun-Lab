package com.hundunlab.site.common.util;

import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;

@Slf4j
//todoed修好了 我也不知道咋回事 反正就是用不了这个了
public class PasswordUtil {

    // 加密强度（4-31，越高越安全）
    private static final int LOG_ROUNDS = 12;

    /**
     * 加密密码
     */
    public static String addMi(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new RuntimeException("密码不能为空");
        }
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(LOG_ROUNDS));
    }

    /**
     * 校验密码（明文与密文匹配）
     */
    public static boolean check(String mingPassword, String encodedPassword) {
//测试通过        log.info("进入校验密码工具类:当前明文密码{},加密密码{}",mingPassword,encodedPassword);
        if (encodedPassword == null || encodedPassword.isEmpty()) {
            return false;
        }
        return BCrypt.checkpw(mingPassword, encodedPassword);
    }
}
