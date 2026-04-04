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

        // First: expire ACTIVE meetups whose OTP has expired (quick set via query)
        int expiredByOtp = meetupRepository.expireMeetupsByOtpExpiry(now, MeetupStatus.EXPIRED);

        // Next: fetch SCHEDULED/READY meetups whose scheduledTime is in the past and check per-meetup timeWindowMinutes
        java.util.List<com.trustmarket.handshake.safe_meetup.entity.Meetup> candidates = meetupRepository.findByStatusInAndScheduledTimeLessThan(
                java.util.List.of(MeetupStatus.SCHEDULED, MeetupStatus.READY), now);

        int expiredByTime = 0;
        for (com.trustmarket.handshake.safe_meetup.entity.Meetup m : candidates) {
            // consider the configured time window: if now is after scheduledTime + timeWindowMinutes, expire it
            java.time.LocalDateTime expiryCutoff = m.getScheduledTime().plusMinutes(m.getTimeWindowMinutes());
            if (now.isAfter(expiryCutoff)) {
                m.setStatus(MeetupStatus.EXPIRED);
                meetupRepository.save(m);
                expiredByTime++;
            }
        }

        if (expiredByTime > 0 || expiredByOtp > 0) {
            log.info("Meetup cleanup expired by time={}, by otp={}", expiredByTime, expiredByOtp);
        }
    }
}

