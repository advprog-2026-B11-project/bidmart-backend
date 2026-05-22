package com.example.bidmart.user.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String verificationSubject;
    private final String mfaSubject;

    public EmailServiceImpl(JavaMailSender mailSender,
                            @Value("${app.email.from}") String fromAddress,
                            @Value("${app.email.verification-subject:Verify your BidMart account}") String verificationSubject,
                            @Value("${app.email.mfa-subject:Your BidMart login code}") String mfaSubject) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.verificationSubject = verificationSubject;
        this.mfaSubject = mfaSubject;
    }
    
    @Async
    @Override
    public void sendVerificationEmail(String toEmail, String verificationUrl) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(
                message,
                MimeMessageHelper.MULTIPART_MODE_NO,
                StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(verificationSubject);
            helper.setText(buildVerificationEmailBody(verificationUrl), true);
        } catch (MessagingException e) {
            throw new IllegalStateException("Failed to compose verification email.", e);
        }
        mailSender.send(message);
    }

    @Async
    @Override
    public void sendMfaCodeEmail(String toEmail, String code) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(
                message,
                MimeMessageHelper.MULTIPART_MODE_NO,
                StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(mfaSubject);
            helper.setText(buildMfaCodeEmailBody(code), true);
        } catch (MessagingException e) {
            throw new IllegalStateException("Failed to compose MFA code email.", e);
        }
        mailSender.send(message);
    }

    private String buildVerificationEmailBody(String verificationUrl) {
        return "<div style=\"font-family:Arial, sans-serif; font-size:14px;\">"
                + "<p>Hi,</p>"
                + "<p>Please verify your BidMart account by clicking the button below.</p>"
                + "<p>"
                + "<a href=\"" + verificationUrl + "\" "
                + "style=\"background:#0b5ed7;color:#ffffff;padding:10px 16px;text-decoration:none;border-radius:4px;display:inline-block;\">"
                + "Verify Email"
                + "</a>"
                + "</p>"
                + "<p>If the button does not work, copy and paste this link into your browser:</p>"
                + "<p>" + verificationUrl + "</p>"
                + "</div>";
    }

    private String buildMfaCodeEmailBody(String code) {
        return "<div style=\"font-family:Arial, sans-serif; font-size:14px;\">"
                + "<p>Hi,</p>"
                + "<p>Use this code to complete your login:</p>"
                + "<p style=\"font-size:20px;font-weight:bold;letter-spacing:2px;\">" + code + "</p>"
                + "<p>If you did not try to log in, you can ignore this email.</p>"
                + "</div>";
    }
}
