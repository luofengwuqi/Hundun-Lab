package com.hundunlab.site.common.util.email;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * SMTP 邮件发送适配层。
 * 全站只在这里碰 JavaMailSender，业务侧只调用 send / sendBatch。
 * 未配置 MAIL_FROM / MAIL_AUTH_CODE 时不阻断应用启动，仅跳过发送并记录告警。
 */
@Slf4j
@Component
public class SendMsgUtil {

    private final JavaMailSender mailSender;
    private final String from;
    private final boolean enabled;

    public SendMsgUtil(JavaMailSender mailSender,
                       @Value("${spring.mail.username:}") String from,
                       @Value("${spring.mail.password:}") String password) {
        this.mailSender = mailSender;
        this.from = from;
        this.enabled = StringUtils.hasText(from) && StringUtils.hasText(password);
        if (!enabled) {
            log.warn("邮件服务未配置（MAIL_FROM / MAIL_AUTH_CODE），邮件通知将跳过发送");
        }
    }

    /**
     * 邮件通道是否可用。业务侧可用它决定是否提示用户"邮件通知不可用"
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 发送单封纯文本邮件
     *
     * @return 是否发送成功（未配置邮件服务时返回 false，不抛异常）
     */
    public boolean send(String to, String subject, String content) {
        if (!enabled) {
            log.warn("邮件服务未配置，跳过发送：to={}, subject={}", to, subject);
            return false;
        }
        try {
            mailSender.send(buildMessage(to, subject, content));
            log.info("邮件发送成功，收件人：{}", to);
            return true;
        } catch (Exception e) {
            // 收件人地址非法、SMTP 拒绝等都属于"这一封发不出去"，不应影响调用方主流程
            log.error("邮件发送失败，收件人：{}，主题：{}", to, subject, e);
            return false;
        }
    }

    /**
     * 批量发送：与逐封发送相比复用同一条 SMTP 连接
     *
     * @return 成功投递的封数
     */
    public int sendBatch(List<String> toList, String subject, String content) {
        if (!enabled) {
            log.warn("邮件服务未配置，跳过批量发送：{} 个收件人", toList == null ? 0 : toList.size());
            return 0;
        }
        if (toList == null || toList.isEmpty()) {
            return 0;
        }
        try {
            MimeMessage[] messages = new MimeMessage[toList.size()];
            for (int i = 0; i < toList.size(); i++) {
                messages[i] = buildMessage(toList.get(i), subject, content);
            }
            mailSender.send(messages);
            log.info("批量邮件发送完成，数量：{}", messages.length);
            return messages.length;
        } catch (Exception e) {
            log.error("批量邮件发送失败，目标数量：{}", toList.size(), e);
            return 0;
        }
    }

    private MimeMessage buildMessage(String to, String subject, String content) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        // 显式指定 UTF-8，避免中文主题与正文乱码
        MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, false);
        return message;
    }
}
