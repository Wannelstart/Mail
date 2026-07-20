package com.mail.dto.request;

import lombok.Data;

@Data
public class SendMailRequest {
    private String receiverEmail;
    private String subject;
    private String content;
    private boolean draft;
}
