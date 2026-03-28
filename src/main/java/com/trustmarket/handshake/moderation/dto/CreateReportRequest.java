package com.trustmarket.handshake.moderation.dto;


import com.trustmarket.handshake.moderation.enums.ReportTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReportRequest {

    @NotNull
    private ReportTargetType targetType;

    @NotNull
    private UUID targetId;

    @NotBlank
    @Size(max = 120)
    private String reason;

    @Size(max = 5000)
    private String description;
}