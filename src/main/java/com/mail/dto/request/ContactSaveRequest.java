package com.mail.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ContactSaveRequest {
    @NotBlank
    private String email;
    private String nickname;
    private String groupName;
}
