CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone_number VARCHAR(20),
    email VARCHAR(255),
    password_hash VARCHAR(255),
    login_provider VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT users_login_provider_check
        CHECK (login_provider IN ('PHONE', 'EMAIL', 'GOOGLE', 'APPLE', 'FACEBOOK')),
    CONSTRAINT users_status_check
        CHECK (status IN ('ACTIVE', 'BLOCKED', 'SUSPENDED', 'DELETED')),
    CONSTRAINT users_login_identifier_check
        CHECK (
            (login_provider = 'PHONE' AND phone_number IS NOT NULL)
            OR (login_provider = 'EMAIL' AND email IS NOT NULL)
            OR (login_provider IN ('GOOGLE', 'APPLE', 'FACEBOOK'))
        ),
    CONSTRAINT users_provider_identity_unique UNIQUE (login_provider, provider_user_id)
);

CREATE UNIQUE INDEX users_phone_number_unique
    ON users (phone_number)
    WHERE phone_number IS NOT NULL;

CREATE UNIQUE INDEX users_email_unique
    ON users (LOWER(email))
    WHERE email IS NOT NULL;

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role),
    CONSTRAINT user_roles_role_check CHECK (role IN ('VIEWER', 'STREAMER'))
);

CREATE TABLE user_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    username VARCHAR(50) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    bio TEXT,
    avatar_url TEXT,
    cover_image_url TEXT,
    gender VARCHAR(30),
    date_of_birth DATE,
    country_code CHAR(2),
    language_code VARCHAR(10),
    is_private BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX user_profiles_username_unique
    ON user_profiles (LOWER(username));

CREATE TABLE devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_id VARCHAR(255) NOT NULL,
    device_name VARCHAR(100),
    platform VARCHAR(20) NOT NULL DEFAULT 'ANDROID',
    app_version VARCHAR(30),
    os_version VARCHAR(30),
    fcm_token TEXT,
    last_active_at TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT devices_platform_check CHECK (platform IN ('ANDROID')),
    CONSTRAINT devices_user_device_unique UNIQUE (user_id, device_id)
);

CREATE INDEX devices_user_active_index ON devices (user_id, is_active);

CREATE TABLE otp_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    destination VARCHAR(255) NOT NULL,
    channel VARCHAR(10) NOT NULL,
    otp_hash CHAR(64) NOT NULL,
    purpose VARCHAR(30) NOT NULL,
    attempt_count SMALLINT NOT NULL DEFAULT 0,
    max_attempts SMALLINT NOT NULL DEFAULT 5,
    expires_at TIMESTAMPTZ NOT NULL,
    verified_at TIMESTAMPTZ,
    invalidated_at TIMESTAMPTZ,
    ip_address VARCHAR(45),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT otp_requests_channel_check CHECK (channel IN ('SMS', 'EMAIL')),
    CONSTRAINT otp_requests_purpose_check CHECK (purpose IN ('LOGIN', 'REGISTER', 'RESET_PASSWORD')),
    CONSTRAINT otp_requests_attempts_check
        CHECK (attempt_count >= 0 AND max_attempts > 0 AND attempt_count <= max_attempts),
    CONSTRAINT otp_requests_expiry_check CHECK (expires_at > created_at)
);

CREATE INDEX otp_requests_lookup_index
    ON otp_requests (destination, purpose, created_at DESC);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    token_hash CHAR(64) NOT NULL UNIQUE,
    token_family_id UUID NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT refresh_tokens_expiry_check CHECK (expires_at > created_at)
);

CREATE INDEX refresh_tokens_active_session_index
    ON refresh_tokens (user_id, device_id, expires_at)
    WHERE revoked_at IS NULL;
