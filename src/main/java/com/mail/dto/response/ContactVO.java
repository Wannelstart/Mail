package com.mail.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ContactVO {
    private Long id;
    private Long contactId;
    private String username;
    private String email;
    private String nickname;
    private String groupName;
    private LocalDateTime createdAt;
}
