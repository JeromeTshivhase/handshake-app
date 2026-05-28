package com.trustmarket.handshake.user.service;

import com.trustmarket.handshake.shared.security.HashUtil;
import com.trustmarket.handshake.user.dto.PayoutMethodDto;
import com.trustmarket.handshake.user.entity.PayoutMethod;
import com.trustmarket.handshake.user.entity.User;
import com.trustmarket.handshake.user.repository.PayoutMethodRepository;
import com.trustmarket.handshake.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PayoutMethodService {

    private final PayoutMethodRepository payoutMethodRepository;
    private final UserRepository userRepository;

    public Optional<PayoutMethod> getByUserId(UUID userId) {
        return payoutMethodRepository.findByUser_Id(userId);
    }

    @Transactional
    public PayoutMethod addOrUpdate(UUID userId, PayoutMethodDto dto) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));

        // normalize account number for hashing
        String normAcct = dto.getAccountNumber() == null ? null : dto.getAccountNumber().replaceAll("\\D+", "");
        String acctHash = normAcct == null ? null : HashUtil.sha256Hex(normAcct);

        // duplicate detection
        if (acctHash != null) {
            boolean exists = payoutMethodRepository.existsByAccountNumberHash(acctHash);
            // if exists but belongs to same user, allow update
            if (exists) {
                Optional<com.trustmarket.handshake.user.entity.PayoutMethod> existing = payoutMethodRepository.findByAccountNumberHash(acctHash);
                if (existing.isPresent() && !existing.get().getUser().getId().equals(userId)) {
                    throw new SecurityException("Account already linked to another user");
                }
            }
        }

        PayoutMethod pm = payoutMethodRepository.findByUser_Id(userId).orElse(PayoutMethod.builder().user(user).build());
        pm.setBankName(dto.getBankName());
        pm.setBranchCode(dto.getBranchCode());
        pm.setAccountNumber(dto.getAccountNumber()); // encrypted by JPA converter
        pm.setAccountNumberHash(acctHash);
        pm.setIdentityFingerprint(dto.getIdentityFingerprint());
        return payoutMethodRepository.save(pm);
    }

    @Transactional
    public void remove(UUID userId) {
        payoutMethodRepository.findByUser_Id(userId).ifPresent(payoutMethodRepository::delete);
    }
}

