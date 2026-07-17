package com.mail.dto.response;

import lombok.Data;

@Data
public class AttachmentInfo {
    private Long id;
    private String fileName;
    private Long fileSize;
    private String contentType;
}
