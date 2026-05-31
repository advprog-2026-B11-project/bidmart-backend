package com.example.bidmart.user.service;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Sends transactional email through the Brevo HTTP API instead of SMTP.
 *
 * <p>Render's free tier blocks the outbound SMTP ports (587/465), so every SMTP-based
 * provider fails in production. Brevo's REST endpoint runs over HTTPS (port 443), which
 * is not blocked, and only requires a single verified sender address (no custom domain).
 */
@Service
public class EmailServiceImpl implements EmailService {

    private final RestClient restClient;
    private final String fromAddress;
    private final String fromName;
    private final String verificationSubject;
    private final String mfaSubject;

    @Autowired
    public EmailServiceImpl(@Value("${app.email.brevo.api-key:}") String brevoApiKey,
                            @Value("${app.email.brevo.url:https://api.brevo.com/v3/smtp/email}") String brevoUrl,
                            @Value("${app.email.from}") String fromAddress,
                            @Value("${app.email.from-name:BidMart}") String fromName,
                            @Value("${app.email.verification-subject:Verify your BidMart account}") String verificationSubject,
                            @Value("${app.email.mfa-subject:Your BidMart login code}") String mfaSubject) {
        this(RestClient.builder(), brevoApiKey, brevoUrl, fromAddress, fromName,
                verificationSubject, mfaSubject);
    }

    // Package-private: lets tests inject a builder bound to MockRestServiceServer.
    EmailServiceImpl(RestClient.Builder restClientBuilder,
                     String brevoApiKey,
                     String brevoUrl,
                     String fromAddress,
                     String fromName,
                     String verificationSubject,
                     String mfaSubject) {
        this.restClient = restClientBuilder
                .baseUrl(brevoUrl)
                .defaultHeader("api-key", brevoApiKey)
                .defaultHeader("accept", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("content-type", MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.fromAddress = fromAddress;
        this.fromName = fromName;
        this.verificationSubject = verificationSubject;
        this.mfaSubject = mfaSubject;
    }

    @Async
    @Override
    public void sendVerificationEmail(String toEmail, String verificationUrl) {
        sendEmail(toEmail, verificationSubject, buildVerificationEmailBody(verificationUrl),
                "verification email");
    }

    @Async
    @Override
    public void sendMfaCodeEmail(String toEmail, String code) {
        sendEmail(toEmail, mfaSubject, buildMfaCodeEmailBody(code), "MFA code email");
    }

    private void sendEmail(String toEmail, String subject, String htmlContent, String description) {
        Map<String, Object> payload = Map.of(
                "sender", Map.of("name", fromName, "email", fromAddress),
                "to", List.of(Map.of("email", toEmail)),
                "subject", subject,
                "htmlContent", htmlContent);
        try {
            restClient.post()
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send " + description + " to " + toEmail, e);
        }
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