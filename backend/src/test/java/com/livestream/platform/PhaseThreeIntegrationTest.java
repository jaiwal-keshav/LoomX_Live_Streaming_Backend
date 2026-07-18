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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class PhaseThreeIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("livestream_test")
            .withUsername("livestream")
            .withPassword("livestream");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("auth.otp.expose-code", () -> true);
        registry.add("agora.app-id", () -> "0123456789abcdef0123456789abcdef");
        registry.add("agora.app-certificate", () -> "abcdef0123456789abcdef0123456789");
    }

    @Autowired
    MockMvc mvc;

    @Test
    void liveStreamSupportsAudienceLikesCohostsInvitationsAndEnd() throws Exception {
        Account host = register("host");
        Account viewer = register("viewer");
        Account invited = register("invited");
        String categoryId = JsonPath.read(authenticatedGet("/api/v1/stream-categories", host.token()), "$[0].id");

        String created = mvc.perform(post("/api/v1/streams")
                        .header("Authorization", bearer(host.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Learning Live","categoryId":"%s"}
                                """.formatted(categoryId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stream.status").value("LIVE"))
                .andExpect(jsonPath("$.rtc.role").value("BROADCASTER"))
                .andReturn().getResponse().getContentAsString();
        String streamId = JsonPath.read(created, "$.stream.id");

        mvc.perform(post("/api/v1/streams")
                        .header("Authorization", bearer(host.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Second Stream","categoryId":"%s"}
                                """.formatted(categoryId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LIVE_STREAM_ALREADY_EXISTS"));

        mvc.perform(post("/api/v1/streams/{id}/join", streamId)
                        .header("Authorization", bearer(viewer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rtc.role").value("AUDIENCE"))
                .andExpect(jsonPath("$.stream.currentViewerCount").value(1));

        mvc.perform(put("/api/v1/streams/{id}/like", streamId)
                        .header("Authorization", bearer(viewer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLikeCount").value(1));
        mvc.perform(put("/api/v1/streams/{id}/like", streamId)
                        .header("Authorization", bearer(viewer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLikeCount").value(1));

        String request = mvc.perform(post("/api/v1/streams/{id}/join-requests", streamId)
                        .header("Authorization", bearer(viewer.token())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String requestId = JsonPath.read(request, "$.id");
        mvc.perform(patch("/api/v1/streams/{id}/join-requests/{requestId}", streamId, requestId)
                        .header("Authorization", bearer(host.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
        mvc.perform(post("/api/v1/streams/{id}/rtc-token", streamId)
                        .header("Authorization", bearer(viewer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("BROADCASTER"));

        String invitation = mvc.perform(post("/api/v1/streams/{id}/invitations", streamId)
                        .header("Authorization", bearer(host.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inviteeId\":\"" + invited.id() + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String invitationId = JsonPath.read(invitation, "$.id");
        mvc.perform(patch("/api/v1/stream-invitations/{id}", invitationId)
                        .header("Authorization", bearer(invited.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
        mvc.perform(post("/api/v1/streams/{id}/rtc-token", streamId)
                        .header("Authorization", bearer(invited.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("BROADCASTER"));

        mvc.perform(post("/api/v1/streams/{id}/end", streamId)
                        .header("Authorization", bearer(viewer.token())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("HOST_REQUIRED"));
        mvc.perform(post("/api/v1/streams/{id}/end", streamId)
                        .header("Authorization", bearer(host.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENDED"))
                .andExpect(jsonPath("$.currentViewerCount").value(0));
        mvc.perform(post("/api/v1/streams/{id}/rtc-token", streamId)
                        .header("Authorization", bearer(viewer.token())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STREAM_NOT_LIVE"));
    }

    private Account register(String prefix) throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = prefix + "-" + suffix + "@example.com";
        String otpResponse = mvc.perform(post("/api/v1/auth/otp/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"channel":"EMAIL","purpose":"REGISTER","destination":"%s"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String requestId = JsonPath.read(otpResponse, "$.requestId");
        String code = JsonPath.read(otpResponse, "$.debugOtp");
        String registration = mvc.perform(post("/api/v1/auth/register/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "otpRequestId":"%s",
                                  "code":"%s",
                                  "password":"strong-password-123",
                                  "profile":{"username":"%s_%s","displayName":"%s"},
                                  "device":{"deviceId":"android-%s"}
                                }
                                """.formatted(requestId, code, prefix, suffix, prefix, suffix)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new Account(JsonPath.read(registration, "$.user.id"), JsonPath.read(registration, "$.accessToken"));
    }

    private String authenticatedGet(String path, String token) throws Exception {
        return mvc.perform(get(path).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record Account(String id, String token) {
    }
}
