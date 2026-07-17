package com.mail.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Contact {
    private Long id;
    private Long ownerId;
    private Long contactId;
    private String email;
    private String nickname;
    private String groupName;
    private LocalDateTime createdAt;
}
