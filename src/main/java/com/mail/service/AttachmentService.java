package com.mail.service;

import com.mail.entity.Attachment;
import com.mail.entity.Mail;
import com.mail.mapper.AttachmentMapper;
import com.mail.mapper.MailMapper;
import com.mail.util.FileUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
@Service
@RequiredArgsConstructor
public class AttachmentService {

  private final AttachmentMapper attachmentMapper;
    private final MailMapper mailMapper;
    private final FileUtil fileUtil;

    @Transactional
    public List<Long> upload(Long mailId, Long userId, List<MultipartFile> files) throws IOException {
        Mail mail = mailMapper.findById(mailId);
        if (mail == null) throw new IllegalArgumentException("邮件不存在");
        if (!userId.equals(mail.getSenderId())) throw new SecurityException("无权操作");

        List<Attachment> list = new ArrayList<>();
        for (MultipartFile file : files) {
          String path = fileUtil.saveFile(file);
            Attachment a = new Attachment();
        a.setMailId(mailId);
            a.setFileName(file.getOriginalFilename());
            a.setFilePath(path);
            a.setFileSize(file.getSize());
         a.setContentType(file.getContentType());
            list.add(a);
        }
        attachmentMapper.insertBatch(list);
        mailMapper.updateHasAttachments(mailId);
        return list.stream().map(Attachment::getId).toList();
    }

    public byte[] download(Long attachmentId, Long userId) throws IOException {
        Attachment a = attachmentMapper.findById(attachmentId);
        if (a == null) throw new IllegalArgumentException("附件不存在");
        Mail mail = mailMapper.findById(a.getMailId());
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
        return fileUtil.readFile(a.getFilePath());
    }

    public Attachment getAttachment(Long attachmentId) {
        return attachmentMapper.findById(attachmentId);
    }
}
