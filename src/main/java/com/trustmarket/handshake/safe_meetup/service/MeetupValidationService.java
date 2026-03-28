package com.trustmarket.handshake.safe_meetup.service;

import com.trustmarket.handshake.safe_meetup.entity.Meetup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class MeetupValidationService {

    @Value("${meetup.radius-meters:200}")
    private double radiusMeters;

    @Value("${meetup.time-window-minutes:30}")
    private long timeWindowMinutes;

    public double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        // Haversine
        final double R = 6371000.0; // meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    public boolean withinTimeWindow(LocalDateTime now, LocalDateTime scheduledTime) {
        Duration d = Duration.between(scheduledTime, now).abs();
        return d.toMinutes() <= timeWindowMinutes;
    }

    public boolean isNear(double userLat, double userLng, Meetup meetup) {
        return distanceMeters(userLat, userLng, meetup.getLatitude(), meetup.getLongitude()) <= radiusMeters;
    }

    public double radiusMeters() {
        return radiusMeters;
    }

    public long timeWindowMinutes() {
        return timeWindowMinutes;
    }
}