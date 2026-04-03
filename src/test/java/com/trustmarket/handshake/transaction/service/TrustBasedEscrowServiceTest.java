package com.trustmarket.handshake.transaction.service;

import com.trustmarket.handshake.transaction.entity.Transaction;
import com.trustmarket.handshake.transaction.repository.TransactionRepository;
import com.trustmarket.handshake.user.entity.User;
import com.trustmarket.handshake.user.service.TrustScoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrustBasedEscrowServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TrustScoreService trustScoreService;

    private TrustBasedEscrowService trustBasedEscrowService;

    @BeforeEach
    void setUp() {
        trustBasedEscrowService = new TrustBasedEscrowService(transactionRepository, trustScoreService);
        ReflectionTestUtils.setField(trustBasedEscrowService, "trustedScoreThreshold", 80);
        ReflectionTestUtils.setField(trustBasedEscrowService, "verifiedScoreThreshold", 60);
        ReflectionTestUtils.setField(trustBasedEscrowService, "standardScoreThreshold", 40);
        ReflectionTestUtils.setField(trustBasedEscrowService, "limitedScoreThreshold", 20);
        ReflectionTestUtils.setField(trustBasedEscrowService, "highValueThreshold", new BigDecimal("5000"));
        ReflectionTestUtils.setField(trustBasedEscrowService, "trustedHoldHours", 0);
        ReflectionTestUtils.setField(trustBasedEscrowService, "verifiedHoldHours", 24);
        ReflectionTestUtils.setField(trustBasedEscrowService, "standardHoldHours", 48);
        ReflectionTestUtils.setField(trustBasedEscrowService, "limitedHoldHours", 168);
        ReflectionTestUtils.setField(trustBasedEscrowService, "restrictedHoldHours", 336);
        ReflectionTestUtils.setField(trustBasedEscrowService, "highValueExtraHoldHours", 24);
    }

    @Test
    void applyHoldPolicyAppliesExpectedHoldByScoreAndAmount() {
        User seller = User.builder()
                .email("seller@example.com")
                .passwordHash("x")
                .firstName("S")
                .lastName("T")
                .build();

        Transaction txn = Transaction.builder()
                .seller(seller)
                .amount(new BigDecimal("7500.00"))
                .build();

        when(trustScoreService.recalculateAndSave(seller)).thenReturn(82);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        int holdHours = trustBasedEscrowService.applyHoldPolicy(txn);

        assertEquals(24, holdHours);
        verify(transactionRepository).save(txn);
    }
}

