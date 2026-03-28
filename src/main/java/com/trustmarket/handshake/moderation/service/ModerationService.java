package com.trustmarket.handshake.moderation.service;

import com.trustmarket.handshake.moderation.dto.CreateReportRequest;
import com.trustmarket.handshake.moderation.entity.Report;
import com.trustmarket.handshake.moderation.enums.ReportStatus;
import com.trustmarket.handshake.moderation.repository.ReportRepository;
import com.trustmarket.handshake.shared.exception.NotFoundException;
import com.trustmarket.handshake.user.entity.User;
import com.trustmarket.handshake.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ModerationService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    @Transactional
    public Report createReport(UUID reporterId, CreateReportRequest req) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new NotFoundException("Reporter not found: " + reporterId));

        Report report = Report.builder()
                .reporter(reporter)
                .targetType(req.getTargetType())
                .targetId(req.getTargetId())
                .reason(req.getReason().trim())
                .description(req.getDescription())
                .build();

        return reportRepository.save(report);
    }

    @Transactional(readOnly = true)
    public Page<Report> listOpen(int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize);
        return reportRepository.findByStatusOrderByCreatedAtDesc(ReportStatus.OPEN, pageable);
    }
}