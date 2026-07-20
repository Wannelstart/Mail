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

    @jakarta.annotation.PostConstruct
    public void recoverPendingMails() {
        List<Mail> pendingMails = mailMapper.findPendingExternalMails();
        if (pendingMails.isEmpty()) return;
        log.info("恢复 {} 封待发送的外部邮件", pendingMails.size());
        for (Mail mail : pendingMails) {
            try {
                User sender = userMapper.findById(mail.getSenderId());
                if (sender == null || sender.getQqEmail() == null) {
                    mailMapper.updateStatus(mail.getId(), MailStatus.FAILED.getCode());
                    continue;
                }
                String qqEmail = sender.getQqEmail();
                String qqAuthCode = aesEncryptor.decrypt(sender.getQqAuthCode());
                String toEmail = mail.getRemoteSenderEmail();
                Long mailId = mail.getId();
                String subject = mail.getSubject();
                String content = mail.getContent();

                java.util.concurrent.ScheduledFuture<?> future = scheduler.schedule(() -> {
                    try {
                        List<com.mail.entity.Attachment> attachments = attachmentMapper.findByMailId(mailId);
                        externalMailService.sendExternalMail(qqEmail, qqAuthCode, toEmail, subject, content, attachments);
                        mailMapper.updateStatus(mailId, MailStatus.SENT.getCode());
                        mailMapper.updateSentAt(mailId, LocalDateTime.now());
                        pendingSends.remove(mailId);
                    } catch (Exception e) {
                        log.error("恢复发送失败: mailId={}", mailId, e);
                        mailMapper.updateStatus(mailId, MailStatus.FAILED.getCode());
                        pendingSends.remove(mailId);
                    }
                }, 1, java.util.concurrent.TimeUnit.MINUTES);
                pendingSends.put(mailId, future);
            } catch (Exception e) {
                log.error("恢复邮件失败: mailId={}", mail.getId(), e);
                mailMapper.updateStatus(mail.getId(), MailStatus.FAILED.getCode());
            }
        }
    }

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
        // Check soft-delete status
        if (userId.equals(mail.getSenderId()) && Boolean.TRUE.equals(mail.getSenderDeleted())) {
            throw new IllegalArgumentException("邮件已删除");
        }
        if (userId.equals(mail.getReceiverId()) && Boolean.TRUE.equals(mail.getReceiverDeleted())) {
            throw new IllegalArgumentException("邮件已删除");
        }
        // Fix #10: Only update read status, preserve existing starred/important
        if (userId.equals(mail.getReceiverId())) {
            com.mail.entity.MailFlag existingFlag = mailFlagMapper.findByMailAndUser(mailId, userId);
            if (existingFlag != null) {
                existingFlag.setRead(true);
                mailFlagMapper.upsert(existingFlag);
            } else {
                com.mail.entity.MailFlag flag = new com.mail.entity.MailFlag();
                flag.setMailId(mailId);
                flag.setUserId(userId);
                flag.setRead(true);
                flag.setStarred(false);
                flag.setImportant(false);
                mailFlagMapper.upsert(flag);
            }
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

        String receiverEmail = req.getReceiverEmail();
        boolean hasReceiver = receiverEmail != null && !receiverEmail.isBlank();

        User receiver = hasReceiver ? userMapper.findByEmail(receiverEmail) : null;
        boolean isExternal = (receiver == null);

        // If draft with no receiver, allow it
        if (req.isDraft() && !hasReceiver) {
            isExternal = false;
        }

        // If not draft and is external, check domain
        if (!req.isDraft() && isExternal && receiverEmail != null && receiverEmail.endsWith("@" + mailDomain)) {
            throw new IllegalArgumentException("收件人不存在");
        }

        Mail mail = new Mail();
        mail.setSenderId(senderId);
        mail.setReceiverId(isExternal ? null : receiver != null ? receiver.getId() : null);
        String subject = req.getSubject();
        String content = req.getContent();
        if (!req.isDraft()) {
            subject = htmlSanitizer.sanitize(subject);
            content = htmlSanitizer.sanitize(content);
        }
        mail.setSubject(subject);
        mail.setContent(content);
        mail.setRemote(isExternal);
        mail.setRemoteSenderEmail(isExternal && hasReceiver ? receiverEmail : null);

        if (req.isDraft()) {
            mail.setStatus(MailStatus.DRAFT.getCode());
        } else if (isExternal) {
            // Fix #4: Set PENDING status for external mail, only mark SENT after SMTP success
            mail.setStatus(MailStatus.PENDING.getCode());
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
                    // Fix #5/#18: Load persisted attachments and pass to SMTP
                    List<com.mail.entity.Attachment> attachments = attachmentMapper.findByMailId(mailId);
                    externalMailService.sendExternalMail(qqEmail, qqAuthCode, toEmail, finalSubject, finalContent, attachments);
                    mailMapper.updateStatus(mailId, MailStatus.SENT.getCode());
                    mailMapper.updateSentAt(mailId, LocalDateTime.now());
                    pendingSends.remove(mailId);
                    log.info("延迟发送完成: mailId={}", mailId);
                } catch (Exception e) {
                    log.error("延迟发送失败: mailId={}", mailId, e);
                    mailMapper.updateStatus(mailId, MailStatus.FAILED.getCode());
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

        String receiverEmail = req.getReceiverEmail();
        boolean hasReceiver = receiverEmail != null && !receiverEmail.isBlank();
        User receiver = hasReceiver ? userMapper.findByEmail(receiverEmail) : null;
        boolean isExternal = hasReceiver && (receiver == null);

        if (isExternal && receiverEmail.endsWith("@" + mailDomain)) {
            throw new IllegalArgumentException("收件人不存在");
        }

        mail.setReceiverId(!hasReceiver ? null : (isExternal ? null : receiver.getId()));
        mail.setSubject(req.getSubject());
        mail.setContent(req.getContent());
        mail.setRemote(isExternal);
        mail.setRemoteSenderEmail(isExternal ? receiverEmail : null);
        mailMapper.updateDraft(mail);

        // If changing from draft to sent
        if (!req.isDraft()) {
            if (!hasReceiver) throw new IllegalArgumentException("收件人不能为空");
            mailMapper.updateToSent(mailId, LocalDateTime.now());
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
        // Fix #11: Verify user has access to source mail
        if (!userId.equals(source.getSenderId()) && !userId.equals(source.getReceiverId())) {
            throw new SecurityException("无权访问源邮件");
        }

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
        // Allow recall for PENDING mails (sentAt is null but mail was "sent")
        if (mail.getStatus() == MailStatus.PENDING.getCode()) {
            // PENDING external mail, allow recall
        } else if (sentAt == null) {
            throw new IllegalArgumentException("该邮件尚未发送");
        }

        if (mail.getStatus() != MailStatus.PENDING.getCode()) {
            LocalDateTime now = LocalDateTime.now();
            long secondsDiff = java.time.Duration.between(sentAt, now).getSeconds();
            if (secondsDiff > 120) {
                throw new IllegalArgumentException("超过撤回时限（只能撤回2分钟内发送的邮件）");
            }
        }

        // 取消延迟发送任务（如果是外部邮件）
        java.util.concurrent.ScheduledFuture<?> future = pendingSends.remove(mailId);
        if (future != null) {
            if (future.isDone()) {
                throw new IllegalArgumentException("邮件已发送，无法撤回");
            }
            future.cancel(false);
            log.info("已取消延迟发送任务: mailId={}", mailId);
        }

        // 撤回到草稿箱：状态改为草稿，删除收件人副本
        mailMapper.recallToDraft(mailId, userId);

        log.info("邮件已撤回到草稿箱: mailId={}, senderId={}", mailId, userId);
    }
}
