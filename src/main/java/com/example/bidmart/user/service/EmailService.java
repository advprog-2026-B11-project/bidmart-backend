package com.example.bidmart.user.service;

public interface EmailService {
    void sendVerificationEmail(String toEmail, String verificationUrl);
    void sendMfaCodeEmail(String toEmail, String code);
    void sendNotificationEmail(String toEmail, String subject, String body);
}
