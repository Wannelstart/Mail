package com.mail.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Component
public class SignatureUtil {

    @Value("${app.remote-api-secret}")
    private String secret;

    public String generate(String senderEmail, String receiverEmail, String timestamp,
                           String nonce, String subject, String content) {
        try {
            // Include all payload fields in signature
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            String subjectHash = Base64.getEncoder().encodeToString(
                    sha256.digest(subject != null ? subject.getBytes(StandardCharsets.UTF_8) : new byte[0]));
            String contentHash = Base64.getEncoder().encodeToString(
                    sha256.digest(content != null ? content.getBytes(StandardCharsets.UTF_8) : new byte[0]));

            String data = senderEmail + ":" + receiverEmail + ":" + timestamp
                    + ":" + (nonce != null ? nonce : "")
                    + ":" + subjectHash + ":" + contentHash;

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("Signature generation failed", e);
        }
    }

    public boolean verify(String senderEmail, String receiverEmail,
                          String timestamp, String signature,
                          String nonce, String subject, String content) {
        String expected = generate(senderEmail, receiverEmail, timestamp, nonce, subject, content);
        // Constant-time comparison to prevent timing attacks
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature != null ? signature.getBytes(StandardCharsets.UTF_8) : new byte[0]);
    }
}
