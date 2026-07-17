package com.mail.controller;

import com.mail.dto.request.SendMailRequest;
import com.mail.dto.response.ApiResponse;
import com.mail.dto.response.MailDetail;
import com.mail.dto.response.MailListItem;
import com.mail.dto.response.PageResult;
import com.mail.security.UserPrincipal;
import com.mail.service.MailService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mail")
@RequiredArgsConstructor
public class MailController {

    private final MailService mailService;

    @GetMapping("/inbox")
    public ApiResponse<PageResult<MailListItem>> inbox(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(mailService.getInbox(user.getId(), page, size));
    }

    @GetMapping("/sent")
    public ApiResponse<PageResult<MailListItem>> sent(
          @AuthenticationPrincipal UserPrincipal user,
         @RequestParam(defaultValue = "1") @Min(1) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(mailService.getSent(user.getId(), page, size));
    }

    @GetMapping("/drafts")
    public ApiResponse<PageResult<MailListItem>> drafts(
         @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
      return ApiResponse.ok(mailService.getDrafts(user.getId(), page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<MailDetail> detail(
            @AuthenticationPrincipal UserPrincipal user,
        @PathVariable Long id) {
        return ApiResponse.ok(mailService.getDetail(id, user.getId()));
    }

    @PostMapping("/send")
    public ApiResponse<Map<String, Long>> send(
      @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody SendMailRequest req) {
      Long mailId = mailService.sendMail(user.getId(), req);
        return ApiResponse.ok(Map.of("id", mailId));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> updateDraft(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id,
            @Valid @RequestBody SendMailRequest req) {
        mailService.updateDraft(id, user.getId(), req);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/copy-attachments")
    public ApiResponse<Void> copyAttachments(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Long sourceMailId = Long.valueOf(body.get("sourceMailId").toString());
        List<Long> attachmentIds = null;
        if (body.containsKey("attachmentIds")) {
            attachmentIds = ((List<Number>) body.get("attachmentIds")).stream()
                    .map(Number::longValue).collect(java.util.stream.Collectors.toList());
        }
        mailService.copyAttachments(sourceMailId, id, user.getId(), attachmentIds);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id) {
        mailService.deleteMail(id, user.getId());
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/recall")
    public ApiResponse<Void> recall(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id) {
        mailService.recallMail(id, user.getId());
        return ApiResponse.ok();
    }
}
