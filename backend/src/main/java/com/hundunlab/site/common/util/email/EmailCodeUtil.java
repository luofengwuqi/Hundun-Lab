package com.hundunlab.site.common.util.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

import static com.hundunlab.site.common.constant.email.TEXT.EMAIL_CODE_CONTENT_PREFIX;
import static com.hundunlab.site.common.constant.email.TEXT.EMAIL_CODE_SUBJECT;
import static com.hundunlab.site.common.constant.redis.RedisConstant.EMAIL_CODE_EXPIRE_TIME;

@Slf4j
@Component
public class EmailCodeUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final SendMsgUtil sendMsgUtil;

    public EmailCodeUtil(SendMsgUtil sendMsgUtil) {
        this.sendMsgUtil = sendMsgUtil;
    }

    /**
     * 生成 6 位随机数字验证码（SecureRandom，不可用 Math.random）
     */
    public String generateCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    /**
     * 发送验证码到指定邮箱
     *
     * @return 是否发送成功
     */
    public boolean sendEmailCode(String toEmail, String code) {
        String content = EMAIL_CODE_CONTENT_PREFIX + code
                + "\n\n（" + EMAIL_CODE_EXPIRE_TIME + " 分钟内有效，请勿转发给他人）";
        return sendMsgUtil.send(toEmail, EMAIL_CODE_SUBJECT, content);
    }
}
