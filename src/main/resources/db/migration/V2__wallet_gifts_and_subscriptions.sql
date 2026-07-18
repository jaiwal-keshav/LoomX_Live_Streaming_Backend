CREATE TABLE wallets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    coin_balance BIGINT NOT NULL DEFAULT 0,
    diamond_balance BIGINT NOT NULL DEFAULT 0,
    subscription_point_balance BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT wallets_balances_non_negative CHECK (
        coin_balance >= 0 AND diamond_balance >= 0 AND subscription_point_balance >= 0
    )
);

CREATE TABLE coin_packages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    coins BIGINT NOT NULL,
    bonus_coins BIGINT NOT NULL DEFAULT 0,
    price_minor BIGINT NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'INR',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT coin_packages_values_positive CHECK (
        coins > 0 AND bonus_coins >= 0 AND price_minor > 0
    )
);

CREATE TABLE subscription_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price_minor BIGINT NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'INR',
    total_points BIGINT NOT NULL,
    validity_days INT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT subscription_plans_values_positive CHECK (
        price_minor > 0 AND total_points > 0 AND (validity_days IS NULL OR validity_days > 0)
    )
);

CREATE TABLE payment_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    product_type VARCHAR(30) NOT NULL,
    product_id UUID NOT NULL,
    grant_currency VARCHAR(30) NOT NULL,
    grant_amount BIGINT NOT NULL,
    amount_minor BIGINT NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'INR',
    provider VARCHAR(20) NOT NULL DEFAULT 'RAZORPAY',
    provider_order_id VARCHAR(100) UNIQUE,
    provider_payment_id VARCHAR(100) UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    idempotency_key VARCHAR(100) NOT NULL,
    failure_reason TEXT,
    captured_at TIMESTAMPTZ,
    refunded_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT payment_orders_product_type_check CHECK (
        product_type IN ('COIN_PACKAGE', 'SUBSCRIPTION_PLAN')
    ),
    CONSTRAINT payment_orders_grant_currency_check CHECK (
        grant_currency IN ('COIN', 'SUBSCRIPTION_POINT')
    ),
    CONSTRAINT payment_orders_status_check CHECK (
        status IN ('CREATED', 'AUTHORIZED', 'CAPTURED', 'FAILED', 'REFUNDED')
    ),
    CONSTRAINT payment_orders_values_positive CHECK (grant_amount > 0 AND amount_minor > 0),
    CONSTRAINT payment_orders_user_idempotency_unique UNIQUE (user_id, idempotency_key)
);

CREATE TABLE payment_webhook_events (
    event_id VARCHAR(100) PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMPTZ,
    processing_error TEXT
);

CREATE TABLE wallet_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id UUID NOT NULL REFERENCES wallets(id),
    currency VARCHAR(30) NOT NULL,
    type VARCHAR(10) NOT NULL,
    amount BIGINT NOT NULL,
    source VARCHAR(30) NOT NULL,
    reference_type VARCHAR(50),
    reference_id UUID,
    balance_before BIGINT NOT NULL,
    balance_after BIGINT NOT NULL,
    idempotency_key VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT wallet_transactions_currency_check CHECK (
        currency IN ('COIN', 'DIAMOND', 'SUBSCRIPTION_POINT')
    ),
    CONSTRAINT wallet_transactions_type_check CHECK (type IN ('CREDIT', 'DEBIT')),
    CONSTRAINT wallet_transactions_source_check CHECK (
        source IN ('PURCHASE', 'GIFT', 'CALL', 'ADMIN_ADJUSTMENT', 'REFUND', 'PROMOTION')
    ),
    CONSTRAINT wallet_transactions_values_check CHECK (
        amount > 0 AND balance_before >= 0 AND balance_after >= 0
    )
);

CREATE UNIQUE INDEX wallet_transactions_idempotency_unique
    ON wallet_transactions (wallet_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX wallet_transactions_history_index
    ON wallet_transactions (wallet_id, created_at DESC);

CREATE TABLE gift_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    icon_url TEXT,
    banner_url TEXT,
    display_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE gifts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID NOT NULL REFERENCES gift_categories(id),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    icon_url TEXT,
    animation_url TEXT,
    coin_cost BIGINT NOT NULL,
    diamond_reward BIGINT NOT NULL,
    rarity VARCHAR(20) NOT NULL DEFAULT 'COMMON',
    is_limited BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT gifts_values_positive CHECK (coin_cost > 0 AND diamond_reward >= 0),
    CONSTRAINT gifts_rarity_check CHECK (rarity IN ('COMMON', 'RARE', 'EPIC', 'LEGENDARY'))
);

CREATE TABLE user_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    plan_id UUID NOT NULL REFERENCES subscription_plans(id),
    payment_order_id UUID UNIQUE REFERENCES payment_orders(id),
    total_points BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    purchased_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT user_subscriptions_points_positive CHECK (total_points > 0),
    CONSTRAINT user_subscriptions_status_check CHECK (
        status IN ('ACTIVE', 'COMPLETED', 'CANCELLED', 'EXPIRED')
    )
);

CREATE INDEX user_subscriptions_user_status_index
    ON user_subscriptions (user_id, status, purchased_at DESC);

CREATE TABLE gift_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id UUID NOT NULL REFERENCES users(id),
    receiver_id UUID NOT NULL REFERENCES users(id),
    stream_id UUID NOT NULL,
    gift_id UUID NOT NULL REFERENCES gifts(id),
    quantity INT NOT NULL,
    total_coin_cost BIGINT NOT NULL,
    total_diamond_reward BIGINT NOT NULL,
    wallet_transaction_debit_id UUID NOT NULL UNIQUE REFERENCES wallet_transactions(id),
    wallet_transaction_credit_id UUID NOT NULL UNIQUE REFERENCES wallet_transactions(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT gift_transactions_users_different CHECK (sender_id <> receiver_id),
    CONSTRAINT gift_transactions_values_positive CHECK (
        quantity > 0 AND total_coin_cost > 0 AND total_diamond_reward >= 0
    )
);
