package com.mail.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Mail {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private String subject;
    private String content;
    private Integer status; // 0=草稿, 1=已发
    private Boolean senderDeleted;
    private Boolean receiverDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private Boolean hasAttachments;
    private Boolean remote;
    private String senderServer;
    private String remoteSenderEmail;
}
