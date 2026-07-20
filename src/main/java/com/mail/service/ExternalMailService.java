package com.mail.service;

import com.mail.entity.Attachment;
import com.mail.util.FileUtil;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalMailService {

    private final FileUtil fileUtil;

    /**
     * 通过用户绑定的 QQ 邮箱 SMTP 发送外部邮件（带附件支持）
     */
    public void sendExternalMail(String fromEmail, String authCode, String toEmail,
                                  String subject, String content, List<Attachment> attachments) {
        JavaMailSender sender = createQqMailSender(fromEmail, authCode);
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(content, false);

            // Add attachments
            if (attachments != null) {
                for (Attachment att : attachments) {
                    try {
                        byte[] data = fileUtil.readFile(att.getFilePath());
                        String contentType = att.getContentType() != null
                                ? att.getContentType() : "application/octet-stream";
                        helper.addAttachment(att.getFileName(),
                                new org.springframework.core.io.ByteArrayResource(data),
                                contentType);
                    } catch (Exception e) {
                        log.warn("添加附件失败: fileName={}, path={}", att.getFileName(), att.getFilePath(), e);
                    }
                }
            }

            sender.send(message);
            log.info("外部邮件发送成功: from={}, to={}, subject={}", fromEmail, toEmail, subject);
        } catch (MessagingException e) {
            log.error("外部邮件发送失败: from={}, to={}, subject={}", fromEmail, toEmail, subject, e);
            throw new RuntimeException("邮件发送失败: " + e.getMessage());
        }
    }

    /**
     * 根据用户的 QQ 邮箱和授权码创建 JavaMailSender
     */
    private JavaMailSender createQqMailSender(String qqEmail, String authCode) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost("smtp.qq.com");
        sender.setPort(465);
        sender.setUsername(qqEmail);
        sender.setPassword(authCode);
        sender.setProtocol("smtp");
        sender.setDefaultEncoding("UTF-8");

        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");

        return sender;
    }
}
