package com.mail.dto.response;

import lombok.Data;

@Data
public class AuthResponse {
    private String token;
    private Long id;
    private String username;
    private String email;

    public static AuthResponse of(String token, Long id, String username, String email) {
        AuthResponse r = new AuthResponse();
        r.token = token;
        r.id = id;
        r.username = username;
        r.email = email;
        return r;
    }
}
