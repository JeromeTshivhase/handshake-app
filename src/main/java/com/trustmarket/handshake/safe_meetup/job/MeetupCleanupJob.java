package com.trustmarket.handshake.safe_meetup.job;

import com.trustmarket.handshake.safe_meetup.entity.MeetupStatus;
import com.trustmarket.handshake.safe_meetup.repository.MeetupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class MeetupCleanupJob {

    private final MeetupRepository meetupRepository;

    @Scheduled(cron = "0 */15 * * * *")
    @Transactional
    public void expireOldMeetups() {
        LocalDateTime now = LocalDateTime.now();
        int expiredByTime = meetupRepository.expireMeetupsByTime(now, MeetupStatus.EXPIRED);
        int expiredByOtp = meetupRepository.expireMeetupsByOtpExpiry(now, MeetupStatus.EXPIRED);

        if (expiredByTime > 0 || expiredByOtp > 0) {
            log.info("Meetup cleanup expired by time={}, by otp={}", expiredByTime, expiredByOtp);
        }
    }
}

