package com.livestream.platform.auth;

import com.livestream.platform.shared.ApiException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InputNormalizerTest {

    @Test
    void normalizesIdentityFields() {
        assertEquals("learner@example.com", InputNormalizer.email(" Learner@Example.com "));
        assertEquals("learner_1", InputNormalizer.username(" Learner_1 "));
        assertEquals("+919876543210", InputNormalizer.phone(" +919876543210 "));
    }

    @Test
    void rejectsUnsafeCredentialsAndUrls() {
        assertThrows(ApiException.class, () -> InputNormalizer.password("short"));
        assertThrows(ApiException.class, () -> InputNormalizer.phone("9876543210"));
        assertThrows(ApiException.class, () -> InputNormalizer.optionalHttpsUrl("http://example.com/a.png", "avatarUrl"));
    }
}
