package com.hundunlab.site.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 邮件服务配置
 * 授权码只从环境变量 MAIL_AUTH_CODE 或本地 application-secret.yml 读取，禁止写死在源码中
 */
@Component
public class MailProperties {

    private final String from;
    private final String authCode;
    private final String smtpHost;

    public MailProperties(@Value("${app.mail.from}") String from,
                          @Value("${app.mail.auth-code:}") String authCode,
                          @Value("${app.mail.smtp-host:smtp.qq.com}") String smtpHost) {
        if (authCode == null || authCode.isBlank()) {
            throw new IllegalStateException("邮箱授权码未配置，请设置环境变量 MAIL_AUTH_CODE");
        }
        this.from = from;
        this.authCode = authCode;
        this.smtpHost = smtpHost;
    }

    public String getFrom() {
        return from;
    }

    public String getAuthCode() {
        return authCode;
    }

    public String getSmtpHost() {
        return smtpHost;
    }
}
