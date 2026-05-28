package com.trustmarket.handshake.user.service;

import com.trustmarket.handshake.user.dto.PayoutMethodDto;
import com.trustmarket.handshake.user.entity.PayoutMethod;
import com.trustmarket.handshake.user.entity.User;
import com.trustmarket.handshake.user.repository.PayoutMethodRepository;
import com.trustmarket.handshake.user.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@SpringBootTest
@Transactional
class PayoutMethodServiceTest {

    @Autowired
    private PayoutMethodService payoutMethodService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PayoutMethodRepository payoutMethodRepository;

    @Test
    void addAndDetectDuplicateAccount() {
        // create user1
        User u1 = User.builder()
                .email("u1@example.com")
                .passwordHash("hash")
                .firstName("A")
                .lastName("B")
                .build();
        u1 = userRepository.save(u1);

        PayoutMethodDto dto = PayoutMethodDto.builder()
                .bankName("Bank")
                .branchCode("000")
                .accountNumber("0123-456-789")
                .identityFingerprint("fp1")
                .build();

        PayoutMethod saved = payoutMethodService.addOrUpdate(u1.getId(), dto);
        Assertions.assertNotNull(saved.getId());
        Assertions.assertTrue(payoutMethodRepository.findByUser_Id(u1.getId()).isPresent());

        // create user2 and attempt to add same account -> should throw SecurityException
        User u2 = User.builder()
                .email("u2@example.com")
                .passwordHash("hash")
                .firstName("C")
                .lastName("D")
                .build();
        u2 = userRepository.save(u2);
        final java.util.UUID u2Id = u2.getId();

        PayoutMethodDto dto2 = PayoutMethodDto.builder()
                .bankName("Bank")
                .branchCode("000")
                .accountNumber("0123456789")
                .identityFingerprint("fp2")
                .build();

        Assertions.assertThrows(SecurityException.class, () -> payoutMethodService.addOrUpdate(u2Id, dto2));
    }
}


