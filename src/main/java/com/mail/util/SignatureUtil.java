package com.mail.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Component
public class SignatureUtil {

    @Value("${app.remote-api-secret}")
    private String secret;

    public String generate(String senderEmail, String receiverEmail, String timestamp) {
        try {
            String data = senderEmail + ":" + receiverEmail + ":" + timestamp;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Signature generation failed", e);
        }
    }

    public boolean verify(String senderEmail, String receiverEmail,
                       String timestamp, String signature) {
        return generate(senderEmail, receiverEmail, timestamp).equals(signature);
    }
}
