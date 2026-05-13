package com.example.bidmart.user.service;

public interface EmailService {
    void sendVerificationEmail(String toEmail, String verificationUrl);
}
