package com.mail.controller;

import com.mail.dto.response.ApiResponse;
import com.mail.entity.Attachment;
import com.mail.security.UserPrincipal;
import com.mail.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attachment")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping("/upload/{mailId}")
    public ApiResponse<Map<String, List<Long>>> upload(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long mailId,
         @RequestParam("files") List<MultipartFile> files) throws IOException {
        List<Long> ids = attachmentService.upload(mailId, user.getId(), files);
        return ApiResponse.ok(Map.of("ids", ids));
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> download(
      @AuthenticationPrincipal UserPrincipal user,
      @PathVariable Long id) throws IOException {
        byte[] data = attachmentService.download(id, user.getId());
        Attachment a = attachmentService.getAttachment(id);
        String encoded = URLEncoder.encode(a.getFileName(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
            .contentType(MediaType.parseMediaType(
                     a.getContentType() != null ? a.getContentType() : "application/octet-stream"))
                .body(data);
    }
}
