package com.trustmarket.handshake.user.controller;

import com.trustmarket.handshake.shared.api.ApiResponse;
import com.trustmarket.handshake.shared.security.AuthUtils;
import com.trustmarket.handshake.user.dto.PayoutMethodDto;
import com.trustmarket.handshake.user.entity.PayoutMethod;
import com.trustmarket.handshake.user.service.PayoutMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payout-methods")
@RequiredArgsConstructor
public class PayoutMethodController {

    private final PayoutMethodService payoutMethodService;

    @GetMapping("/me")
    public ApiResponse<PayoutMethodDto> getMine(Authentication auth) {
        UUID userId = AuthUtils.userId(auth);
        return payoutMethodService.getByUserId(userId)
                .map(pm -> ApiResponse.ok(PayoutMethodDto.builder()
                        .id(pm.getId())
                        .bankName(pm.getBankName())
                        .branchCode(pm.getBranchCode())
                        .accountNumber(null)
                        .identityFingerprint(pm.getIdentityFingerprint())
                        .build()))
                .orElse(ApiResponse.ok(null));
    }

    @PostMapping("/me")
    public ApiResponse<PayoutMethodDto> upsertMine(Authentication auth, @RequestBody PayoutMethodDto dto) {
        UUID userId = AuthUtils.userId(auth);
        PayoutMethod saved = payoutMethodService.addOrUpdate(userId, dto);
        PayoutMethodDto out = PayoutMethodDto.builder()
                .id(saved.getId())
                .bankName(saved.getBankName())
                .branchCode(saved.getBranchCode())
                .accountNumber(null)
                .identityFingerprint(saved.getIdentityFingerprint())
                .build();
        return ApiResponse.ok(out);
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> deleteMine(Authentication auth) {
        UUID userId = AuthUtils.userId(auth);
        payoutMethodService.remove(userId);
        return ApiResponse.ok(null);
    }
}

