package com.trustmarket.handshake.user.entity;

import com.trustmarket.handshake.shared.security.EncryptionConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payout_method", indexes = {
        @Index(name = "idx_payout_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayoutMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "bank_name", length = 200)
    private String bankName;

    @Column(name = "branch_code", length = 50)
    private String branchCode;

    // Encrypted account number
    @Convert(converter = EncryptionConverter.class)
    @Column(name = "account_number", length = 100)
    private String accountNumber;

    // Blind index for account number (SHA-256 hex) to allow duplicate detection without decrypting
    @Column(name = "account_number_hash", length = 128)
    private String accountNumberHash;

    // Identity fingerprint hash (blind index) - e.g. hash of ID number
    @Column(name = "identity_fingerprint", length = 128)
    private String identityFingerprint;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

