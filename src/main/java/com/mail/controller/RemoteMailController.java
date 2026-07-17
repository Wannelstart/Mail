package com.mail.controller;

import com.mail.dto.request.RemoteReceiveRequest;
import com.mail.dto.response.ApiResponse;
import com.mail.service.RemoteMailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/remote")
@RequiredArgsConstructor
public class RemoteMailController {

    private final RemoteMailService remoteMailService;

    @PostMapping("/receive")
    public ApiResponse<Map<String, Long>> receive(@RequestBody RemoteReceiveRequest req) throws IOException {
        Long id = remoteMailService.receive(req);
        return ApiResponse.ok(Map.of("id", id));
    }
}
