package com.mail.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendMailRequest {
    @NotBlank
    private String receiverEmail;
    private String subject;
    private String content;
    private boolean draft;
}
