package com.mail.service;

import com.mail.dto.request.SendMailRequest;
import com.mail.dto.response.AttachmentInfo;
import com.mail.dto.response.MailDetail;
import com.mail.dto.response.MailListItem;
import com.mail.dto.response.PageResult;
import com.mail.entity.Attachment;
import com.mail.entity.Mail;
import com.mail.entity.User;
import com.mail.mapper.AttachmentMapper;
import com.mail.mapper.MailFlagMapper;
import com.mail.mapper.MailMapper;
import com.mail.mapper.UserMapper;
import com.mail.entity.MailStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final MailMapper mailMapper;
    private final UserMapper userMapper;
    private final AttachmentMapper attachmentMapper;
    private final MailFlagMapper mailFlagMapper;
    private final ExternalMailService externalMailService;
    private final com.mail.util.HtmlSanitizer htmlSanitizer;
    private final com.mail.util.AesEncryptor aesEncryptor;

    // 延迟发送任务管理
    private final java.util.concurrent.ConcurrentHashMap<Long, java.util.concurrent.ScheduledFuture<?>> pendingSends = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ScheduledExecutorService scheduler = java.util.concurrent.Executors.newScheduledThreadPool(2);

    @org.springframework.beans.factory.annotation.Value("${app.mail-domain}")
    private String mailDomain;

    public PageResult<MailListItem> getInbox(Long userId, int page, int size) {
        int offset = (page - 1) * size;
        List<MailListItem> list = mailMapper.findInbox(userId, offset, size);
        long total = mailMapper.countInbox(userId);
        return PageResult.of(total, page, size, list);
    }

    public PageResult<MailListItem> getSent(Long userId, int page, int size) {
        int offset = (page - 1) * size;
      List<MailListItem> list = mailMapper.findSent(userId, offset, size);
        long total = mailMapper.countSent(userId);
      return PageResult.of(total, page, size, list);
    }

    public PageResult<MailListItem> getDrafts(Long userId, int page, int size) {
      int offset = (page - 1) * size;
        List<MailListItem> list = mailMapper.findDrafts(userId, offset, size);
        long total = mailMapper.countDrafts(userId);
        return PageResult.of(total, page, size, list);
    }

    public MailDetail getDetail(Long mailId, Long userId) {
      Mail mail = mailMapper.findById(mailId);
        if (mail == null) throw new IllegalArgumentException("邮件不存在");
        // 权限检查
        if (!userId.equals(mail.getSenderId()) && !userId.equals(mail.getReceiverId())) {
            throw new SecurityException("无权访问");
        }
        // 自动标记已读
        if (userId.equals(mail.getReceiverId())) {
            com.mail.entity.MailFlag flag = new com.mail.entity.MailFlag();
            flag.setMailId(mailId);
            flag.setUserId(userId);
            flag.setRead(true);
            flag.setStarred(false);
            flag.setImportant(false);
            mailFlagMapper.upsert(flag);
        }
        MailDetail detail = new MailDetail();
        detail.setId(mail.getId());
        detail.setSubject(mail.getSubject());
        detail.setContent(mail.getContent());
        detail.setCreatedAt(mail.getCreatedAt());
        detail.setSentAt(mail.getSentAt());
        detail.setRemote(mail.getRemote());
        detail.setRemoteSenderEmail(mail.getRemoteSenderEmail());
        // 发件人信息
        if (mail.getSenderId() != null) {
            User sender = userMapper.findById(mail.getSenderId());
            if (sender != null) {
                detail.setSenderEmail(sender.getEmail());
                detail.setSenderUsername(sender.getUsername());
            }
        } else if (mail.getRemoteSenderEmail() != null) {
            // 外部发件人（sender_id 为 null）
            detail.setSenderEmail(mail.getRemoteSenderEmail());
            detail.setSenderUsername("外部发件人");
        }
        // 收件人信息
        if (mail.getReceiverId() != null) {
            User receiver = userMapper.findById(mail.getReceiverId());
            if (receiver != null) detail.setReceiverEmail(receiver.getEmail());
        } else if (mail.getRemoteSenderEmail() != null) {
            detail.setReceiverEmail(mail.getRemoteSenderEmail());
        }
        // 附件
        List<Attachment> attachments = attachmentMapper.findByMailId(mailId);
        detail.setAttachments(attachments.stream().map(a -> {
       AttachmentInfo info = new AttachmentInfo();
            info.setId(a.getId());
            info.setFileName(a.getFileName());
            info.setFileSize(a.getFileSize());
            info.setContentType(a.getContentType());
          return info;
        }).collect(Collectors.toList()));
        return detail;
    }
    @Transactional
    public Long sendMail(Long senderId, SendMailRequest req) {
        log.info("发送邮件: senderId={}, receiverEmail={}, isDraft={}", senderId, req.getReceiverEmail(), req.isDraft());
        User receiver = userMapper.findByEmail(req.getReceiverEmail());

        boolean isExternal = (receiver == null);

        // 如果是本站域名但用户不存在，报错
        if (isExternal && req.getReceiverEmail().endsWith("@" + mailDomain)) {
            throw new IllegalArgumentException("收件人不存在");
        }

        Mail mail = new Mail();
        mail.setSenderId(senderId);
        mail.setReceiverId(isExternal ? null : receiver.getId());
        String subject = req.getSubject();
        String content = req.getContent();
        if (!req.isDraft()) {
            subject = htmlSanitizer.sanitize(subject);
            content = htmlSanitizer.sanitize(content);
        }
        mail.setSubject(subject);
        mail.setContent(content);
        mail.setRemote(isExternal);
        mail.setRemoteSenderEmail(isExternal ? req.getReceiverEmail() : null);

        if (req.isDraft()) {
            mail.setStatus(MailStatus.DRAFT.getCode());
        } else {
            mail.setStatus(MailStatus.SENT.getCode());
            mail.setSentAt(LocalDateTime.now());
        }
        mailMapper.insert(mail);
        log.info("邮件保存成功: mailId={}, status={}, external={}", mail.getId(), mail.getStatus(), isExternal);

        // 非草稿且是外部邮件，延迟2分钟发送（支持撤回）
        if (!req.isDraft() && isExternal) {
            String toEmail = req.getReceiverEmail();
            User sender = userMapper.findById(senderId);
            String qqEmail = sender.getQqEmail();
            String qqAuthCode = aesEncryptor.decrypt(sender.getQqAuthCode());
            Long mailId = mail.getId();
            final String finalSubject = subject;
            final String finalContent = content;

            java.util.concurrent.ScheduledFuture<?> future = scheduler.schedule(() -> {
                try {
                    List<Attachment> attachments = attachmentMapper.findByMailId(mailId);
                    externalMailService.sendExternalMail(
                            qqEmail, qqAuthCode, toEmail, finalSubject, finalContent, attachments);
                    pendingSends.remove(mailId);
                    log.info("延迟发送完成: mailId={}", mailId);
                } catch (Exception e) {
                    log.error("延迟发送失败: mailId={}", mailId, e);
                    pendingSends.remove(mailId);
                }
            }, 2, java.util.concurrent.TimeUnit.MINUTES);

            pendingSends.put(mailId, future);
            log.info("邮件已安排延迟发送: mailId={}, 2分钟后发送", mailId);
        }

        return mail.getId();
    }

    @Transactional
    public void updateDraft(Long mailId, Long userId, SendMailRequest req) {
        Mail mail = mailMapper.findById(mailId);
        if (mail == null) throw new IllegalArgumentException("邮件不存在");
        if (!userId.equals(mail.getSenderId())) throw new SecurityException("无权操作");
        if (mail.getStatus() != MailStatus.DRAFT.getCode()) throw new IllegalArgumentException("只能编辑草稿");

        // If changing from draft to sent
        if (!req.isDraft()) {
            User receiver = userMapper.findByEmail(req.getReceiverEmail());
            if (receiver == null) throw new IllegalArgumentException("收件人不存在");
            mail.setReceiverId(receiver.getId());
            mail.setSubject(req.getSubject());
            mail.setContent(req.getContent());
            mailMapper.updateDraft(mail);
            mailMapper.updateToSent(mailId, LocalDateTime.now());
        } else {
            User receiver = userMapper.findByEmail(req.getReceiverEmail());
            if (receiver == null) throw new IllegalArgumentException("收件人不存在");
            mail.setReceiverId(receiver.getId());
            mail.setSubject(req.getSubject());
            mail.setContent(req.getContent());
            mailMapper.updateDraft(mail);
        }
    }

    @Transactional
    public void copyAttachments(Long sourceMailId, Long targetMailId, Long userId, List<Long> attachmentIds) {
        // Verify user owns target mail
        Mail target = mailMapper.findById(targetMailId);
        if (target == null) throw new IllegalArgumentException("目标邮件不存在");
        if (!userId.equals(target.getSenderId())) throw new SecurityException("无权操作");

        // Verify source mail exists
        Mail source = mailMapper.findById(sourceMailId);
        if (source == null) throw new IllegalArgumentException("源邮件不存在");

        if (attachmentIds != null && !attachmentIds.isEmpty()) {
            attachmentMapper.copySelected(sourceMailId, targetMailId, attachmentIds);
        } else {
            attachmentMapper.copyFromMail(sourceMailId, targetMailId);
        }
        mailMapper.updateHasAttachments(targetMailId);
    }

    @Transactional
    public void deleteMail(Long mailId, Long userId) {
     Mail mail = mailMapper.findById(mailId);
        if (mail == null) throw new IllegalArgumentException("邮件不存在");
        if (userId.equals(mail.getSenderId())) {
            mailMapper.softDeleteBySender(mailId, userId);
        } else if (userId.equals(mail.getReceiverId())) {
            mailMapper.softDeleteByReceiver(mailId, userId);
        } else {
            throw new SecurityException("无权操作");
        }
    }

    @Transactional
    public void recallMail(Long mailId, Long userId) {
        Mail mail = mailMapper.findById(mailId);
        if (mail == null) throw new IllegalArgumentException("邮件不存在");

        // 只发送者可以撤回
        if (!userId.equals(mail.getSenderId())) {
            throw new SecurityException("无权撤回他人的邮件");
        }

        // 检查是否在2分钟内
        LocalDateTime sentAt = mail.getSentAt();
        if (sentAt == null) {
            throw new IllegalArgumentException("该邮件尚未发送");
        }

        LocalDateTime now = LocalDateTime.now();
        long minutesDiff = java.time.Duration.between(sentAt, now).toMinutes();
        if (minutesDiff > 2) {
            throw new IllegalArgumentException("超过撤回时限（只能撤回2分钟内发送的邮件）");
        }

        // 取消延迟发送任务（如果是外部邮件）
        java.util.concurrent.ScheduledFuture<?> future = pendingSends.remove(mailId);
        if (future != null) {
            future.cancel(false);
            log.info("已取消延迟发送任务: mailId={}", mailId);
        }

        // 撤回到草稿箱：状态改为草稿，删除收件人副本
        mailMapper.recallToDraft(mailId, userId);

        log.info("邮件已撤回到草稿箱: mailId={}, senderId={}", mailId, userId);
    }
}
