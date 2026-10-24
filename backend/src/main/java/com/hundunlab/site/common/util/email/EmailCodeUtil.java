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
import java.security.SecureRandom;
import java.util.Properties;

import static com.hundunlab.site.common.constant.redis.RedisConstant.EMAIL_CODE_EXPIRE_TIME;

@Slf4j
@Component
public class EmailCodeUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final MailProperties mailProperties;

    public EmailCodeUtil(MailProperties mailProperties) {
        this.mailProperties = mailProperties;
    }

    /**
     * 生成 6 位随机数字验证码
     */
    public String generateCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    /**
     * 发送验证码到指定邮箱
     * @param toEmail 接收方邮箱
     * @param code 验证码
     * @return 发送结果
     */
    public boolean sendEmailCode(String toEmail, String code) {
        // 1. 配置 SMTP 服务器参数
        Properties props = new Properties();
        props.setProperty("mail.smtp.host", mailProperties.getSmtpHost());
        props.setProperty("mail.smtp.port", "587");
        props.setProperty("mail.smtp.auth", "true");
        props.setProperty("mail.smtp.starttls.enable", "true");

        // 2. 创建邮箱会话（登录邮箱）
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(mailProperties.getFrom(), mailProperties.getAuthCode());
            }
        });

        try {
            // 3. 构建邮件内容
            String content = "Ciallo～(∠・ω< )⌒★\n\n您期待已久的验证码是------\n\n" + code + "\n\n只在"+EMAIL_CODE_EXPIRE_TIME+" 分钟内有效!\n\n祝您今天也有好心情!";
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(mailProperties.getFrom())); // 发件人
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail)); // 收件人
            message.setSubject("【身份验证】验证码!"); // 邮件标题
            message.setText(content); // 邮件内容

            // 4. 发送邮件
            Transport.send(message);
            log.info("验证码邮件发送成功，收件人：{}", toEmail);
            return true;
        } catch (MessagingException e) {
            log.error("发送邮件失败，邮箱：{}", toEmail, e);
            return false;
        }
    }

}
