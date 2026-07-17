package com.mail.controller;

import com.mail.dto.response.ApiResponse;
import com.mail.dto.response.MailListItem;
import com.mail.dto.response.PageResult;
import com.mail.security.UserPrincipal;
import com.mail.service.SearchService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ApiResponse<PageResult<MailListItem>> search(
            @AuthenticationPrincipal UserPrincipal user,
        @RequestParam String keyword,
            @RequestParam(defaultValue = "all") String box,
            @RequestParam(defaultValue = "all") String searchIn,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(searchService.search(user.getId(), keyword, box, searchIn, page, size));
    }
}
