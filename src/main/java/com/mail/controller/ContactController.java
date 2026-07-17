package com.mail.controller;

import com.mail.dto.request.ContactSaveRequest;
import com.mail.dto.response.ApiResponse;
import com.mail.dto.response.ContactVO;
import com.mail.security.UserPrincipal;
import com.mail.service.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @GetMapping
    public ApiResponse<List<ContactVO>> list(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) String groupName) {
        return ApiResponse.ok(contactService.list(user.getId(), groupName));
    }

    @GetMapping("/search")
    public ApiResponse<List<ContactVO>> search(
          @AuthenticationPrincipal UserPrincipal user,
          @RequestParam String keyword) {
        return ApiResponse.ok(contactService.search(user.getId(), keyword));
    }

    @PostMapping
    public ApiResponse<ContactVO> add(
            @AuthenticationPrincipal UserPrincipal user,
        @Valid @RequestBody ContactSaveRequest req) {
        return ApiResponse.ok(contactService.add(user.getId(), req));
    }

    @PutMapping("/{id}")
    public ApiResponse<ContactVO> update(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id,
       @Valid @RequestBody ContactSaveRequest req) {
      return ApiResponse.ok(contactService.update(id, user.getId(), req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id) {
        contactService.delete(id, user.getId());
        return ApiResponse.ok();
    }
}
