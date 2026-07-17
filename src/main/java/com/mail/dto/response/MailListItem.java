package com.mail.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MailListItem {
    private Long id;
    private String subject;
    private String senderEmail;
    private String senderUsername;
    private String receiverEmail;
    private String receiverUsername;
    private LocalDateTime sentAt;
    private Boolean hasAttachments;
    private Boolean read;
    private Boolean starred;
    private Boolean important;
    private Boolean remote;
    private String remoteSenderEmail;
}
