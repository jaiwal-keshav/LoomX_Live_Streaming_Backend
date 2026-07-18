CREATE TABLE call_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    caller_id UUID NOT NULL REFERENCES users(id),
    receiver_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMPTZ,
    CONSTRAINT call_requests_users_different CHECK (caller_id <> receiver_id),
    CONSTRAINT call_requests_status_check CHECK (
        status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'CANCELLED', 'EXPIRED')
    )
);

CREATE TABLE call_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    call_request_id UUID UNIQUE REFERENCES call_requests(id),
    room_id VARCHAR(255) NOT NULL UNIQUE,
    caller_id UUID NOT NULL REFERENCES users(id),
    receiver_id UUID NOT NULL REFERENCES users(id),
    call_type VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'INITIATED',
    subscription_id UUID REFERENCES user_subscriptions(id),
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    connected_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    duration_seconds INT NOT NULL DEFAULT 0,
    points_consumed BIGINT NOT NULL DEFAULT 0,
    end_reason VARCHAR(30),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT call_sessions_users_different CHECK (caller_id <> receiver_id),
    CONSTRAINT call_sessions_type_check CHECK (call_type IN ('VOICE', 'VIDEO')),
    CONSTRAINT call_sessions_status_check CHECK (
        status IN ('INITIATED', 'RINGING', 'ACCEPTED', 'REJECTED', 'MISSED', 'ENDED', 'CANCELLED')
    ),
    CONSTRAINT call_sessions_end_reason_check CHECK (
        end_reason IS NULL OR end_reason IN ('USER_ENDED', 'DECLINED', 'TIMEOUT', 'NETWORK_ERROR')
    ),
    CONSTRAINT call_sessions_values_non_negative CHECK (duration_seconds >= 0 AND points_consumed >= 0)
);

CREATE INDEX call_sessions_caller_history_index ON call_sessions (caller_id, created_at DESC);
CREATE INDEX call_sessions_receiver_history_index ON call_sessions (receiver_id, created_at DESC);

CREATE TABLE follows (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    follower_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    followed_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT follows_users_different CHECK (follower_id <> followed_user_id),
    CONSTRAINT follows_unique UNIQUE (follower_id, followed_user_id)
);

CREATE TABLE user_blocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    blocker_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    blocked_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT user_blocks_users_different CHECK (blocker_id <> blocked_user_id),
    CONSTRAINT user_blocks_unique UNIQUE (blocker_id, blocked_user_id)
);

CREATE TABLE reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id UUID NOT NULL REFERENCES users(id),
    target_type VARCHAR(20) NOT NULL,
    target_id VARCHAR(255) NOT NULL,
    reason VARCHAR(30) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    assigned_admin_id UUID,
    resolution_notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT reports_target_type_check CHECK (target_type IN ('USER', 'STREAM', 'MESSAGE')),
    CONSTRAINT reports_reason_check CHECK (reason IN ('SPAM', 'ABUSE', 'HARASSMENT', 'OTHER')),
    CONSTRAINT reports_status_check CHECK (status IN ('PENDING', 'REVIEWED', 'RESOLVED', 'REJECTED'))
);

CREATE TABLE stream_moderation_actions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stream_id UUID NOT NULL REFERENCES streams(id) ON DELETE CASCADE,
    target_user_id UUID NOT NULL REFERENCES users(id),
    actor_user_id UUID NOT NULL REFERENCES users(id),
    action VARCHAR(20) NOT NULL,
    reason TEXT,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT stream_moderation_actions_users_different CHECK (target_user_id <> actor_user_id),
    CONSTRAINT stream_moderation_actions_action_check CHECK (action IN ('KICK', 'MUTE', 'BAN'))
);

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    image_url TEXT,
    deep_link VARCHAR(500),
    reference_type VARCHAR(30),
    reference_id VARCHAR(255),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMPTZ,
    delivery_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count INT NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMPTZ,
    last_error TEXT,
    sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT notifications_delivery_status_check CHECK (
        delivery_status IN ('PENDING', 'SENT', 'FAILED')
    ),
    CONSTRAINT notifications_attempt_count_check CHECK (attempt_count >= 0)
);

CREATE INDEX notifications_user_created_index ON notifications (user_id, created_at DESC);

CREATE TABLE notification_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    stream_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    message_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    gift_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    follow_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    call_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    promotional_notifications BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
