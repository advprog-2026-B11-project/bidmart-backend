package com.example.bidmart.user.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class EmailServiceImplTest {

    private static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";
    private static final String API_KEY = "test-api-key";

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer server;
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
        emailService = new EmailServiceImpl(
                restClientBuilder,
                API_KEY,
                BREVO_URL,
                "no-reply@example.com",
                "BidMart",
                "Verify",
                "Login code");
    }

    @Test
    void sendVerificationEmail_postsToBrevoWithExpectedPayload() {
        server.expect(requestTo(BREVO_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", API_KEY))
                .andExpect(jsonPath("$.sender.email").value("no-reply@example.com"))
                .andExpect(jsonPath("$.sender.name").value("BidMart"))
                .andExpect(jsonPath("$.to[0].email").value("alice@mail.com"))
                .andExpect(jsonPath("$.subject").value("Verify"))
                .andExpect(jsonPath("$.htmlContent").exists())
                .andRespond(withSuccess("{\"messageId\":\"1\"}", MediaType.APPLICATION_JSON));

        emailService.sendVerificationEmail("alice@mail.com", "http://example.com/verify");

        server.verify();
    }

    @Test
    void sendMfaCodeEmail_postsToBrevoWithExpectedPayload() {
        server.expect(requestTo(BREVO_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", API_KEY))
                .andExpect(jsonPath("$.to[0].email").value("bob@mail.com"))
                .andExpect(jsonPath("$.subject").value("Login code"))
                .andExpect(jsonPath("$.htmlContent").exists())
                .andRespond(withSuccess("{\"messageId\":\"2\"}", MediaType.APPLICATION_JSON));

        emailService.sendMfaCodeEmail("bob@mail.com", "123456");

        server.verify();
    }

    @Test
    void sendVerificationEmail_wrapsServerErrorInIllegalState() {
        server.expect(requestTo(BREVO_URL))
                .andRespond(withServerError());

        assertThatThrownBy(() ->
                emailService.sendVerificationEmail("alice@mail.com", "http://example.com/verify"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("alice@mail.com");
    }
}
