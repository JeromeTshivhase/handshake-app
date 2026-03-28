package com.trustmarket.handshake.moderation.repository;

import com.trustmarket.handshake.moderation.entity.Report;
import com.trustmarket.handshake.moderation.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {
    Page<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);
}