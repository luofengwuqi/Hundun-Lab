package com.hundunlab.site.common.util.email;

import com.hundunlab.site.common.config.MailProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.List;
import java.util.Properties;

@Slf4j
@Component
public class SendMsgUtil {

    private final MailProperties mailProperties;

    public SendMsgUtil(MailProperties mailProperties) {
        this.mailProperties = mailProperties;
    }

    private Session createSession() {
        Properties props = new Properties();
        props.setProperty("mail.smtp.host", mailProperties.getSmtpHost());
        props.setProperty("mail.smtp.port", "587");
        props.setProperty("mail.smtp.auth", "true");
        props.setProperty("mail.smtp.starttls.enable", "true");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(mailProperties.getFrom(), mailProperties.getAuthCode());
            }
        });
    }

    public int sendEmailToEmailList(List<String> toEmailList, String content) {
        // 1. 创建会话（仅创建一次）
        Session session = createSession();

        Transport transport = null;
        int successCount = 0;

        try {
            // 核心优化：仅建立一次连接，循环复用发送
            transport = session.getTransport();
            transport.connect();
            log.info("批量发送邮件连接建立成功，待发送邮箱数量：{}", toEmailList.size());

            // 循环发送
            for (String toEmail : toEmailList) {
                try {
                    // 构建邮件：直接使用传入的自定义内容
                    Message message = new MimeMessage(session);
                    message.setFrom(new InternetAddress(mailProperties.getFrom()));
                    message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
                    message.setSubject("【收到反馈】"); // 可自行修改标题
                    message.setText(content); // 使用外部传入的文本

                    // 复用连接发送
                    transport.sendMessage(message, message.getAllRecipients());
                    successCount++;
                    log.info("批量发送邮件成功！邮箱：{}", toEmail);
                } catch (Exception e) {
                    log.error("批量发送邮件失败，邮箱：{}", toEmail, e);
                }
            }
        } catch (Exception e) {
            log.error("批量发送邮件建立连接失败", e);
        } finally {
            // 统一关闭连接
            if (transport != null && transport.isConnected()) {
                try {
                    transport.close();
                    log.info("批量发送邮件连接已关闭");
                } catch (MessagingException e) {
                    log.error("关闭邮件连接失败", e);
                }
            }
        }

        log.info("批量发送完成，总数量：{}，成功数量：{}", toEmailList.size(), successCount);
        return successCount;
    }

    /**
     * 发送单个邮件
     * @param toEmail 收件人邮箱
     * @param content 邮件内容
     * @return 是否发送成功
     */
    public boolean sendSingleEmail(String toEmail, String content) {
        // 1. 创建会话
        Session session = createSession();

        Transport transport = null;

        try {
            // 3. 建立连接
            transport = session.getTransport();
            transport.connect();
            log.info("发送邮件连接建立成功，收件人：{}", toEmail);

            // 4. 构建邮件
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(mailProperties.getFrom()));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject("【反馈回执】");
            message.setText(content);

            // 5. 发送邮件
            transport.sendMessage(message, message.getAllRecipients());
            log.info("邮件发送成功！收件人：{}", toEmail);
            return true;
        } catch (Exception e) {
            log.error("邮件发送失败，收件人：{}", toEmail, e);
            return false;
        } finally {
            // 6. 关闭连接
            if (transport != null && transport.isConnected()) {
                try {
                    transport.close();
                    log.info("发送邮件连接已关闭");
                } catch (MessagingException e) {
                    log.error("关闭邮件连接失败", e);
                }
            }
        }
    }

}
