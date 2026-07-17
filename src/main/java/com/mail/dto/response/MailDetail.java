package com.mail.dto.response;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class MailDetail extends MailListItem {
    private String content;
    private LocalDateTime createdAt;
    private List<AttachmentInfo> attachments;
}
