package com.mail.service;

import com.mail.entity.Attachment;
import com.mail.entity.Mail;
import com.mail.entity.MailStatus;
import com.mail.entity.User;
import com.mail.mapper.AttachmentMapper;
import com.mail.mapper.MailMapper;
import com.mail.mapper.UserMapper;
import com.mail.util.AesEncryptor;
import com.mail.util.FileUtil;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeUtility;
import jakarta.mail.search.FlagTerm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Properties;

@Slf4j
@Service
public class IncomingMailService {

    private final MailMapper mailMapper;
    private final UserMapper userMapper;
    private final AttachmentMapper attachmentMapper;
    private final AesEncryptor aesEncryptor;
    private final com.mail.util.HtmlSanitizer htmlSanitizer;
    private final FileUtil fileUtil;

    private static final String QQ_IMAP_HOST = "imap.qq.com";
    private static final int QQ_IMAP_PORT = 993;

    public IncomingMailService(MailMapper mailMapper, UserMapper userMapper,
                               AttachmentMapper attachmentMapper,
                               AesEncryptor aesEncryptor,
                               com.mail.util.HtmlSanitizer htmlSanitizer,
                               FileUtil fileUtil) {
        this.mailMapper = mailMapper;
        this.userMapper = userMapper;
        this.attachmentMapper = attachmentMapper;
        this.aesEncryptor = aesEncryptor;
        this.htmlSanitizer = htmlSanitizer;
        this.fileUtil = fileUtil;
    }

    public void fetchAndProcessMails() {
        List<User> users = userMapper.findAllWithQqBinding();
        if (users.isEmpty()) {
            log.debug("没有绑定QQ邮箱的用户，跳过拉取");
            return;
        }

        log.info("开始拉取QQ邮件，共 {} 个绑定了QQ邮箱的用户", users.size());
        for (User user : users) {
            try {
                fetchForUser(user);
            } catch (Exception e) {
                log.error("拉取用户 {} (QQ: {}) 的邮件失败", user.getUsername(), user.getQqEmail(), e);
            }
        }
    }

    private void fetchForUser(User user) {
        String qqEmail = user.getQqEmail();
        String authCode;
        try {
            authCode = aesEncryptor.decrypt(user.getQqAuthCode());
        } catch (Exception e) {
            log.error("解密用户 {} 的QQ授权码失败", user.getUsername(), e);
            return;
        }

        Store store = null;
        Folder inbox = null;
        try {
            Properties props = new Properties();
            props.put("mail.store.protocol", "imaps");
            props.put("mail.imaps.host", QQ_IMAP_HOST);
            props.put("mail.imaps.port", String.valueOf(QQ_IMAP_PORT));
            props.put("mail.imaps.ssl.enable", "true");
            props.put("mail.imaps.ssl.trust", QQ_IMAP_HOST);
            props.put("mail.imaps.connectiontimeout", "15000");
            props.put("mail.imaps.timeout", "30000");

            Session session = Session.getInstance(props);
            store = session.getStore("imaps");
            store.connect(QQ_IMAP_HOST, QQ_IMAP_PORT, qqEmail, authCode);

            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            FlagTerm unreadTerm = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
            Message[] messages = inbox.search(unreadTerm);

            if (messages.length == 0) {
                return;
            }

            log.info("用户 {} (QQ: {}) 有 {} 封未读邮件", user.getUsername(), qqEmail, messages.length);
            int processed = 0;

            for (Message message : messages) {
                try {
                    processMessage(message, user);
                    processed++;
                    // Fix #7: Only mark as read AFTER successful processing
                    try {
                        message.setFlag(Flags.Flag.SEEN, true);
                    } catch (Exception ignored) {}
                } catch (Exception e) {
                    log.error("处理用户 {} 的邮件时出错 (Message-ID: {})", user.getUsername(), getMessageId(message), e);
                    // Do NOT mark as read — leave for retry on next sync
                }
            }

            log.info("用户 {} 邮件拉取完成: 处理 {} 封", user.getUsername(), processed);

        } catch (Exception e) {
            log.error("连接用户 {} (QQ: {}) 的IMAP失败: {}", user.getUsername(), qqEmail, e.getMessage());
        } finally {
            closeQuietly(inbox);
            closeQuietly(store);
        }
    }

    private void processMessage(Message message, User receiver) throws Exception {
        String subject = decodeSubject(message);
        String fromEmail = extractFromEmail(message);
        String content = extractContent(message);
        LocalDateTime sentAt = extractDate(message);

        Mail mail = new Mail();
        mail.setSenderId(null);
        mail.setReceiverId(receiver.getId());
        mail.setSubject(subject);
        String sanitizedContent = htmlSanitizer.sanitize(content);
        mail.setContent(sanitizedContent);
        mail.setStatus(MailStatus.SENT.getCode());
        mail.setRemote(true);
        mail.setRemoteSenderEmail(fromEmail);
        mail.setSentAt(sentAt);

        mailMapper.insert(mail);

        // Fix #8: Extract and save attachments
        List<Attachment> attachments = extractAttachments(message, mail.getId());
        if (!attachments.isEmpty()) {
            attachmentMapper.insertBatch(attachments);
            mailMapper.updateHasAttachments(mail.getId());
        }

        log.info("QQ邮件已保存: mailId={}, userId={}, from={}, to={}, subject={}, attachments={}",
                mail.getId(), receiver.getId(), fromEmail, receiver.getQqEmail(), subject, attachments.size());
    }

    // Fix #8: Recursive content extraction that handles nested multiparts
    private String extractContent(Part part) {
        try {
            String contentType = part.getContentType();
            if (contentType == null) return "";

            Object contentObj = part.getContent();
            if (contentObj instanceof String text) {
                if (contentType.toLowerCase().contains("text/plain")) {
                    return text;
                } else if (contentType.toLowerCase().contains("text/html")) {
                    return text;
                }
                return text;
            } else if (contentObj instanceof Multipart multipart) {
                String htmlText = null;
                String plainText = null;
                for (int i = 0; i < multipart.getCount(); i++) {
                    BodyPart bodyPart = multipart.getBodyPart(i);
                    String bpContentType = bodyPart.getContentType();
                    if (bpContentType == null) continue;
                    String ctLower = bpContentType.toLowerCase();

                    // Recurse into nested multiparts (e.g. multipart/alternative inside multipart/mixed)
                    if (bodyPart.getContent() instanceof Multipart) {
                        String nested = extractContent(bodyPart);
                        if (nested != null && !nested.isEmpty()) {
                            if (htmlText == null) htmlText = nested;
                        }
                        continue;
                    }

                    if (ctLower.contains("text/html")) {
                        htmlText = extractContent(bodyPart);
                    } else if (ctLower.contains("text/plain")) {
                        plainText = extractContent(bodyPart);
                    }
                }
                return htmlText != null ? htmlText : (plainText != null ? plainText : "");
            }
        } catch (Exception e) {
            log.warn("提取邮件内容失败", e);
        }
        return "";
    }

    // Fix #8: Extract attachments from MIME message
    private List<Attachment> extractAttachments(Message message, Long mailId) {
        List<Attachment> result = new ArrayList<>();
        try {
            extractAttachmentsFromPart(message, mailId, result);
        } catch (Exception e) {
            log.warn("提取邮件附件失败", e);
        }
        return result;
    }

    private void extractAttachmentsFromPart(Part part, Long mailId, List<Attachment> result) throws Exception {
        if (part.getContent() instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                extractAttachmentsFromPart(multipart.getBodyPart(i), mailId, result);
            }
            return;
        }

        // Check if this part is an attachment
        String disposition = part.getDisposition();
        String fileName = part.getFileName();
        boolean isAttachment = (disposition != null &&
                (disposition.equalsIgnoreCase(Part.ATTACHMENT) || disposition.equalsIgnoreCase(Part.INLINE)));

        if (!isAttachment && fileName == null) return;

        // Decode filename
        if (fileName != null) {
            fileName = MimeUtility.decodeText(fileName);
        } else {
            fileName = "unnamed";
        }

        String contentType = part.getContentType();
        InputStream is = part.getInputStream();
        byte[] data = is.readAllBytes();
        is.close();

        // Save file using FileUtil's saveBase64File mechanism
        String path = fileUtil.saveBase64File(Base64.getEncoder().encodeToString(data), fileName);

        Attachment a = new Attachment();
        a.setMailId(mailId);
        a.setFileName(fileName);
        a.setFilePath(path);
        a.setFileSize((long) data.length);
        a.setContentType(contentType);
        result.add(a);
    }

    private String decodeSubject(Message message) {
        try {
            String subject = message.getSubject();
            return subject != null ? subject : "(无主题)";
        } catch (Exception e) {
            log.warn("解码邮件主题失败", e);
            return "(无主题)";
        }
    }

    private String extractFromEmail(Message message) {
        try {
            Address[] fromAddresses = message.getFrom();
            if (fromAddresses != null && fromAddresses.length > 0) {
                if (fromAddresses[0] instanceof InternetAddress internetAddress) {
                    return internetAddress.getAddress();
                }
            }
        } catch (Exception e) {
            log.warn("提取发件人失败", e);
        }
        return "unknown@unknown";
    }

    private LocalDateTime extractDate(Message message) {
        try {
            Date receivedDate = message.getReceivedDate();
            if (receivedDate != null) {
                return receivedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            }
            Date sentDate = message.getSentDate();
            if (sentDate != null) {
                return sentDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            }
        } catch (Exception e) {
            log.warn("提取邮件日期失败", e);
        }
        return LocalDateTime.now();
    }

    private String getMessageId(Message message) {
        try {
            String[] headers = message.getHeader("Message-ID");
            if (headers != null && headers.length > 0) return headers[0];
        } catch (Exception ignored) {}
        return "unknown";
    }

    private void closeQuietly(Folder folder) {
        if (folder != null && folder.isOpen()) {
            try {
                folder.close(false);
            } catch (Exception e) {
                log.warn("关闭 IMAP Folder 失败", e);
            }
        }
    }

    private void closeQuietly(Store store) {
        if (store != null && store.isConnected()) {
            try {
                store.close();
            } catch (Exception e) {
                log.warn("关闭 IMAP Store 失败", e);
            }
        }
    }
}
