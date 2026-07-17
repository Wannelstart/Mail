package com.mail.controller;

import com.mail.dto.request.MailFlagRequest;
import com.mail.dto.response.ApiResponse;
import com.mail.security.UserPrincipal;
import com.mail.service.MailFlagService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/mail")
@RequiredArgsConstructor
public class MailFlagController {

    private final MailFlagService mailFlagService;

    @PutMapping("/{id}/flag")
    public ApiResponse<Void> updateFlag(
            @AuthenticationPrincipal UserPrincipal user,
       @PathVariable Long id,
            @RequestBody MailFlagRequest req) {
        mailFlagService.updateFlag(id, user.getId(), req);
        return ApiResponse.ok();
    }
}
