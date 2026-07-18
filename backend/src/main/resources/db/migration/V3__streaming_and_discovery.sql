CREATE TABLE stream_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    icon_url TEXT,
    banner_url TEXT,
    display_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE streams (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id VARCHAR(255) NOT NULL UNIQUE,
    host_id UUID NOT NULL REFERENCES users(id),
    category_id UUID REFERENCES stream_categories(id),
    title VARCHAR(200) NOT NULL,
    description TEXT,
    thumbnail_url TEXT,
    stream_type VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    max_broadcasters INT NOT NULL DEFAULT 5,
    current_viewer_count BIGINT NOT NULL DEFAULT 0,
    total_gift_count BIGINT NOT NULL DEFAULT 0,
    total_gift_coin_value BIGINT NOT NULL DEFAULT 0,
    total_like_count BIGINT NOT NULL DEFAULT 0,
    total_watch_seconds BIGINT NOT NULL DEFAULT 0,
    total_unique_viewers BIGINT NOT NULL DEFAULT 0,
    started_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT streams_type_check CHECK (stream_type IN ('PUBLIC', 'PRIVATE')),
    CONSTRAINT streams_status_check CHECK (status IN ('CREATED', 'LIVE', 'ENDED', 'CANCELLED')),
    CONSTRAINT streams_broadcaster_limit_check CHECK (max_broadcasters BETWEEN 1 AND 5),
    CONSTRAINT streams_counters_non_negative CHECK (
        current_viewer_count >= 0 AND total_gift_count >= 0 AND total_gift_coin_value >= 0
        AND total_like_count >= 0 AND total_watch_seconds >= 0 AND total_unique_viewers >= 0
    )
);

ALTER TABLE gift_transactions
    ADD CONSTRAINT gift_transactions_stream_fk FOREIGN KEY (stream_id) REFERENCES streams(id);

CREATE UNIQUE INDEX streams_one_live_per_host
    ON streams (host_id)
    WHERE status = 'LIVE';

CREATE INDEX streams_discovery_index
    ON streams (status, category_id, started_at DESC);

CREATE TABLE stream_participants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stream_id UUID NOT NULL REFERENCES streams(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    participant_type VARCHAR(20) NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    left_at TIMESTAMPTZ,
    CONSTRAINT stream_participants_type_check CHECK (
        participant_type IN ('HOST', 'CO_HOST', 'VIEWER')
    ),
    CONSTRAINT stream_participants_time_check CHECK (left_at IS NULL OR left_at >= joined_at)
);

CREATE UNIQUE INDEX stream_participants_one_active
    ON stream_participants (stream_id, user_id)
    WHERE left_at IS NULL;

CREATE INDEX stream_participants_stream_active_index
    ON stream_participants (stream_id, participant_type)
    WHERE left_at IS NULL;

CREATE TABLE stream_join_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stream_id UUID NOT NULL REFERENCES streams(id) ON DELETE CASCADE,
    requester_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMPTZ,
    responded_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT stream_join_requests_status_check CHECK (
        status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED', 'EXPIRED')
    )
);

CREATE UNIQUE INDEX stream_join_requests_one_pending
    ON stream_join_requests (stream_id, requester_id)
    WHERE status = 'PENDING';

CREATE TABLE stream_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stream_id UUID NOT NULL REFERENCES streams(id) ON DELETE CASCADE,
    inviter_id UUID NOT NULL REFERENCES users(id),
    invitee_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    sent_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT stream_invitations_users_different CHECK (inviter_id <> invitee_id),
    CONSTRAINT stream_invitations_status_check CHECK (
        status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'CANCELLED', 'EXPIRED')
    )
);

CREATE UNIQUE INDEX stream_invitations_one_pending
    ON stream_invitations (stream_id, invitee_id)
    WHERE status = 'PENDING';

CREATE TABLE stream_broadcaster_slots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stream_id UUID NOT NULL REFERENCES streams(id) ON DELETE CASCADE,
    slot_number INT NOT NULL,
    occupied_by UUID REFERENCES users(id),
    role VARCHAR(20),
    joined_at TIMESTAMPTZ,
    left_at TIMESTAMPTZ,
    CONSTRAINT stream_broadcaster_slots_number_check CHECK (slot_number BETWEEN 1 AND 5),
    CONSTRAINT stream_broadcaster_slots_role_check CHECK (role IS NULL OR role IN ('HOST', 'CO_HOST')),
    CONSTRAINT stream_broadcaster_slots_occupancy_check CHECK (
        (occupied_by IS NULL AND role IS NULL AND joined_at IS NULL)
        OR (occupied_by IS NOT NULL AND role IS NOT NULL AND joined_at IS NOT NULL)
    ),
    CONSTRAINT stream_broadcaster_slots_stream_slot_unique UNIQUE (stream_id, slot_number)
);

CREATE UNIQUE INDEX stream_broadcaster_slots_one_active_user
    ON stream_broadcaster_slots (stream_id, occupied_by)
    WHERE occupied_by IS NOT NULL AND left_at IS NULL;

CREATE TABLE stream_likes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stream_id UUID NOT NULL REFERENCES streams(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT stream_likes_user_unique UNIQUE (stream_id, user_id)
);
