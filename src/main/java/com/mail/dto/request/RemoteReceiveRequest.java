package com.mail.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class RemoteReceiveRequest {
    private String senderEmail;
    private String senderServer;
    private String receiverEmail;
    private String subject;
    private String content;
    private String timestamp;
    private String signature;
    private String nonce;
    private List<RemoteAttachment> attachments;

    @Data
    public static class RemoteAttachment {
        private String fileName;
        private String contentType;
        private long fileSize;
        private String base64Data;
    }
}
