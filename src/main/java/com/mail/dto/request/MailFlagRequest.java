package com.mail.dto.request;

import lombok.Data;

@Data
public class MailFlagRequest {
    private Boolean read;
    private Boolean starred;
    private Boolean important;
}
