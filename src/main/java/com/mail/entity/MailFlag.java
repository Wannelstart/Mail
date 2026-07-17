package com.mail.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MailFlag {
    private Long id;
    private Long mailId;
    private Long userId;
    private Boolean read;
    private Boolean starred;
    private Boolean important;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
