package com.trustmarket.handshake.moderation.dto;

import com.trustmarket.handshake.moderation.entity.*;
import com.trustmarket.handshake.moderation.enums.ReportStatus;
import com.trustmarket.handshake.moderation.enums.ReportTargetType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponse {
    private UUID id;
    private UUID reporterId;
    private ReportTargetType targetType;
    private UUID targetId;
    private String reason;
    private String description;
    private ReportStatus status;
    private String adminNotes;
    private LocalDateTime createdAt;

    public static ReportResponse from(Report r) {
        return ReportResponse.builder()
                .id(r.getId())
                .reporterId(r.getReporter().getId())
                .targetType(r.getTargetType())
                .targetId(r.getTargetId())
                .reason(r.getReason())
                .description(r.getDescription())
                .status(r.getStatus())
                .adminNotes(r.getAdminNotes())
                .createdAt(r.getCreatedAt())
                .build();
    }
}