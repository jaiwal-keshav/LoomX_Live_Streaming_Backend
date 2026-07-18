package com.livestream.platform.streaming;

import com.livestream.platform.config.AgoraProperties;
import com.livestream.platform.shared.ApiException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgoraTokenServiceTest {

    @Test
    void issuesAccountBoundPublisherAndSubscriberTokens() {
        AgoraTokenService service = new AgoraTokenService(new AgoraProperties(
                "0123456789abcdef0123456789abcdef",
                "abcdef0123456789abcdef0123456789",
                Duration.ofMinutes(15)));
        UUID userId = UUID.randomUUID();

        StreamModels.RtcCredentials publisher = service.issue("ls_test", userId, true);
        StreamModels.RtcCredentials subscriber = service.issue("ls_test", userId, false);

        assertTrue(publisher.token().startsWith("007"));
        assertEquals(userId.toString(), publisher.userAccount());
        assertEquals("BROADCASTER", publisher.role());
        assertEquals("AUDIENCE", subscriber.role());
        assertNotEquals(publisher.token(), subscriber.token());
    }

    @Test
    void rejectsTokenRequestsWithoutServerCredentials() {
        AgoraTokenService service = new AgoraTokenService(
                new AgoraProperties("", "", Duration.ofMinutes(15)));

        ApiException exception = assertThrows(ApiException.class,
                () -> service.issue("ls_test", UUID.randomUUID(), false));

        assertEquals("AGORA_NOT_CONFIGURED", exception.code());
    }
}
