package com.livestream.platform.auth;

import com.livestream.platform.shared.ApiException;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

public final class InputNormalizer {
    private static final Pattern PHONE = Pattern.compile("^\\+[1-9]\\d{7,14}$");
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern USERNAME = Pattern.compile("^[a-z0-9_]{3,30}$");

    private InputNormalizer() {
    }

    public static String phone(String value) {
        String normalized = value == null ? "" : value.trim();
        if (!PHONE.matcher(normalized).matches()) {
            throw badRequest("INVALID_PHONE", "Phone number must use E.164 format");
        }
        return normalized;
    }

    public static String email(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 255 || !EMAIL.matcher(normalized).matches()) {
            throw badRequest("INVALID_EMAIL", "Email address is invalid");
        }
        return normalized;
    }

    public static String username(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (!USERNAME.matcher(normalized).matches()) {
            throw badRequest("INVALID_USERNAME", "Username must contain 3-30 lowercase letters, numbers, or underscores");
        }
        return normalized;
    }

    public static String displayName(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty() || normalized.length() > 100) {
            throw badRequest("INVALID_DISPLAY_NAME", "Display name must contain 1-100 characters");
        }
        return normalized;
    }

    public static String password(String value) {
        int byteLength = value == null ? 0 : value.getBytes(StandardCharsets.UTF_8).length;
        if (byteLength < 8 || byteLength > 72) {
            throw badRequest("INVALID_PASSWORD", "Password must contain 8-72 UTF-8 bytes");
        }
        return value;
    }

    public static String optionalHttpsUrl(String value, String fieldName) {
        if (value == null || value.isBlank()) return null;
        try {
            URI uri = URI.create(value.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
                throw new IllegalArgumentException();
            }
            return uri.toString();
        } catch (IllegalArgumentException exception) {
            throw badRequest("INVALID_URL", fieldName + " must be a valid HTTPS URL");
        }
    }

    private static ApiException badRequest(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }
}
