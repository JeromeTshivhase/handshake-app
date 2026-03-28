package com.trustmarket.handshake.moderation.controller;

import com.trustmarket.handshake.moderation.dto.CreateReportRequest;
import com.trustmarket.handshake.moderation.dto.ReportResponse;
import com.trustmarket.handshake.moderation.service.ModerationService;
import com.trustmarket.handshake.shared.api.ApiResponse;
import com.trustmarket.handshake.shared.security.AuthUtils;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/moderation")
@RequiredArgsConstructor
public class ModerationController {

    private final ModerationService moderationService;

    @PostMapping(value = "/reports", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Report content", operationId = "reportContent")
    public ApiResponse<ReportResponse> report(Authentication auth, @Valid @RequestBody CreateReportRequest req) {
        var reporterId = AuthUtils.userId(auth);
        return ApiResponse.ok(ReportResponse.from(moderationService.createReport(reporterId, req)));
    }

    // For now no admin auth gating here; later we restrict to ADMIN role
    @GetMapping(value = "/reports/open", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List open reports", operationId = "listOpenReports")
    public ApiResponse<Page<ReportResponse>> open(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int pageSize
    ) {
        return ApiResponse.ok(moderationService.listOpen(page, pageSize).map(ReportResponse::from));
    }
}