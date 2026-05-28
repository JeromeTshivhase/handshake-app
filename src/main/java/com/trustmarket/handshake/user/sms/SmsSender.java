package com.trustmarket.handshake.user.sms;

import com.trustmarket.handshake.user.entity.User;

/**
 * Abstraction for sending SMS messages.
 */
public interface SmsSender {
    /**
     * Send an SMS message to the given phone number.
     * @param user optional user context
     * @param phone E.164 or provider-acceptable phone number
     * @param message message body
     */
    void sendSms(User user, String phone, String message);
}

