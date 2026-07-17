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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RemoteMailService {

    private final MailMapper mailMapper;
    private final UserMapper userMapper;
    private final AttachmentMapper attachmentMapper;
    private final FileUtil fileUtil;
    private final SignatureUtil signatureUtil;

    @Transactional
    public Long receive(RemoteReceiveRequest req) throws IOException {
        // 验签
        if (!signatureUtil.verify(req.getSenderEmail(), req.getReceiverEmail(),
             req.getTimestamp(), req.getSignature())) {
            throw new SecurityException("签名验证失败");
        }
        // 查找本地收件人
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

        // 处理附件
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
