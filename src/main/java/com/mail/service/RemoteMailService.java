package com.mail.service;

import com.mail.dto.request.RemoteReceiveRequest;
import com.mail.entity.Attachment;
import com.mail.entity.Mail;
import com.mail.entity.User;
import com.mail.mapper.AttachmentMapper;
import com.mail.mapper.MailMapper;
import com.mail.mapper.UserMapper;
import com.mail.util.FileUtil;
import com.mail.util.SignatureUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RemoteMailService {

    private final MailMapper mailMapper;
    private final UserMapper userMapper;
    private final AttachmentMapper attachmentMapper;
    private final FileUtil fileUtil;
    private final SignatureUtil signatureUtil;

    // Simple nonce deduplication (in-memory, resets on restart)
    private final Set<String> seenNonces = ConcurrentHashMap.newKeySet();

    @Transactional
    public Long receive(RemoteReceiveRequest req) throws IOException {
        // Fix #3: Verify signature covers all payload fields
        if (!signatureUtil.verify(req.getSenderEmail(), req.getReceiverEmail(),
                req.getTimestamp(), req.getSignature(),
                req.getNonce(), req.getSubject(), req.getContent())) {
            throw new SecurityException("签名验证失败");
        }

        // Fix #3: Timestamp expiry check (5 minutes)
        if (req.getTimestamp() != null) {
            try {
                long ts = Long.parseLong(req.getTimestamp());
                long now = System.currentTimeMillis();
                if (Math.abs(now - ts) > 5 * 60 * 1000) {
                    throw new SecurityException("请求已过期");
                }
            } catch (NumberFormatException e) {
                throw new SecurityException("无效的时间戳");
            }
        }

        // Fix #3: Nonce deduplication
        if (req.getNonce() != null && !req.getNonce().isEmpty()) {
            if (!seenNonces.add(req.getNonce())) {
                throw new SecurityException("重复的请求");
            }
        }

        // Find local receiver
        User receiver = userMapper.findByEmail(req.getReceiverEmail());
        if (receiver == null) throw new IllegalArgumentException("收件人不存在");

        Mail mail = new Mail();
        mail.setSenderId(null);
        mail.setReceiverId(receiver.getId());
        mail.setSubject(req.getSubject());
        mail.setContent(req.getContent());
        mail.setStatus(1);
        mail.setSentAt(LocalDateTime.now());
        mail.setRemote(true);
        mail.setSenderServer(req.getSenderServer());
        mail.setRemoteSenderEmail(req.getSenderEmail());
        mailMapper.insert(mail);

        // Handle attachments
        if (req.getAttachments() != null && !req.getAttachments().isEmpty()) {
            List<Attachment> list = new ArrayList<>();
            for (RemoteReceiveRequest.RemoteAttachment ra : req.getAttachments()) {
                String path = fileUtil.saveBase64File(ra.getBase64Data(), ra.getFileName());
                Attachment a = new Attachment();
                a.setMailId(mail.getId());
                a.setFileName(ra.getFileName());
                a.setFilePath(path);
                a.setFileSize(ra.getFileSize());
                a.setContentType(ra.getContentType());
                list.add(a);
            }
            attachmentMapper.insertBatch(list);
            mailMapper.updateHasAttachments(mail.getId());
        }
        return mail.getId();
    }
}
