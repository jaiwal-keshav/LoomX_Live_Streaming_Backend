package com.livestream.platform;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class PhaseOneIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("livestream_test")
            .withUsername("livestream")
            .withPassword("livestream");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("auth.otp.expose-code", () -> true);
    }

    @Autowired
    MockMvc mvc;

    @Test
    void emailRegistrationCreatesProfileWalletAndRotatingSession() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "learner-" + suffix + "@example.com";
        Otp otp = requestOtp("EMAIL", "REGISTER", email);

        String registration = mvc.perform(post("/api/v1/auth/register/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "otpRequestId":"%s",
                                  "code":"%s",
                                  "password":"strong-password-123",
                                  "profile":{"username":"learner_%s","displayName":"Learner"},
                                  "device":{"deviceId":"android-%s","deviceName":"Pixel","appVersion":"1.0","osVersion":"15"}
                                }
                                """.formatted(otp.requestId(), otp.code(), suffix, suffix)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email").value(email))
                .andReturn().getResponse().getContentAsString();

        String accessToken = JsonPath.read(registration, "$.accessToken");
        String firstRefreshToken = JsonPath.read(registration, "$.refreshToken");

        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.username").value("learner_" + suffix))
                .andExpect(jsonPath("$.wallet.coins").value(0))
                .andExpect(jsonPath("$.wallet.diamonds").value(0))
                .andExpect(jsonPath("$.wallet.subscriptionPoints").value(0));

        String refresh = mvc.perform(post("/api/v1/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + firstRefreshToken + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String secondRefreshToken = JsonPath.read(refresh, "$.refreshToken");

        mvc.perform(post("/api/v1/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + firstRefreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + secondRefreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void phoneOtpIsSingleUse() throws Exception {
        String suffix = "%07d".formatted(Math.floorMod(UUID.randomUUID().hashCode(), 10_000_000));
        String phone = "+919" + suffix;
        Otp otp = requestOtp("SMS", "REGISTER", phone);
        String body = """
                {
                  "otpRequestId":"%s",
                  "code":"%s",
                  "profile":{"username":"phone_%s","displayName":"Phone Learner"},
                  "device":{"deviceId":"android-phone-%s"}
                }
                """.formatted(otp.requestId(), otp.code(), suffix, suffix);

        mvc.perform(post("/api/v1/auth/register/phone").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/auth/register/phone").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void profileRequiresBearerToken() throws Exception {
        mvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
    }

    private Otp requestOtp(String channel, String purpose, String destination) throws Exception {
        String response = mvc.perform(post("/api/v1/auth/otp/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"channel":"%s","purpose":"%s","destination":"%s"}
                                """.formatted(channel, purpose, destination)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.debugOtp").isString())
                .andReturn().getResponse().getContentAsString();
        return new Otp(JsonPath.read(response, "$.requestId"), JsonPath.read(response, "$.debugOtp"));
    }

    private record Otp(String requestId, String code) {
    }
}
