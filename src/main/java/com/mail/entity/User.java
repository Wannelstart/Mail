package com.mail.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String username;
    private String email;
    private String passwordHash;
    private String salt;
    private String qqEmail;
    private String qqAuthCode;
    private String avatar;
    private LocalDateTime createdAt;
}
