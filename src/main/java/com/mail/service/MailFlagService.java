package com.mail.service;

import com.mail.dto.request.MailFlagRequest;
import com.mail.entity.Mail;
import com.mail.entity.MailFlag;
import com.mail.mapper.MailFlagMapper;
import com.mail.mapper.MailMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailFlagService {

    private final MailFlagMapper mailFlagMapper;
    private final MailMapper mailMapper;

    public void updateFlag(Long mailId, Long userId, MailFlagRequest req) {
    Mail mail = mailMapper.findById(mailId);
        if (mail == null) throw new IllegalArgumentException("邮件不存在");
        if (!userId.equals(mail.getSenderId()) && !userId.equals(mail.getReceiverId())) {
            throw new SecurityException("无权操作");
        }
        MailFlag flag = mailFlagMapper.findByMailAndUser(mailId, userId);
      if (flag == null) {
     flag = new MailFlag();
            flag.setMailId(mailId);
            flag.setUserId(userId);
            flag.setRead(false);
       flag.setStarred(false);
         flag.setImportant(false);
        }
        if (req.getRead() != null) flag.setRead(req.getRead());
        if (req.getStarred() != null) flag.setStarred(req.getStarred());
        if (req.getImportant() != null) flag.setImportant(req.getImportant());
        mailFlagMapper.upsert(flag);
    }
}
