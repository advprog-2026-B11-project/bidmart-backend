package com.example.bidmart.user.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Test
    void sendEmails_shouldInvokeMailSender() {
        MimeMessage message1 = new MimeMessage((Session) null);
        MimeMessage message2 = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message1, message2);

        EmailServiceImpl emailService = new EmailServiceImpl(
                mailSender,
                "no-reply@example.com",
                "Verify",
                "Login code");

        emailService.sendVerificationEmail("alice@mail.com", "http://example.com/verify");
        emailService.sendMfaCodeEmail("alice@mail.com", "123456");

        verify(mailSender, times(1)).send(message1);
        verify(mailSender, times(1)).send(message2);
    }
}
