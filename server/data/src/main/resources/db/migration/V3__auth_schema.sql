-- V3__auth_schema.sql: Authentication & User Identity schema (Spec 004)

-- ── users ────────────────────────────────────────────────────────────────────
CREATE TABLE users (
    id                  BIGSERIAL       PRIMARY KEY,
    username            VARCHAR(30)     NOT NULL,
    email               VARCHAR(255)    NOT NULL,
    email_verified      BOOLEAN         NOT NULL DEFAULT FALSE,
    password_hash       VARCHAR(255),
    role                VARCHAR(20)     NOT NULL DEFAULT 'PARTICIPANT',
    failed_login_count  INTEGER         NOT NULL DEFAULT 0,
    locked_until        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX users_username_idx ON users (username);
CREATE UNIQUE INDEX users_email_idx    ON users (lower(email));

-- ── user_profiles ────────────────────────────────────────────────────────────
CREATE TABLE user_profiles (
    id           BIGSERIAL       PRIMARY KEY,
    user_id      BIGINT          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    display_name VARCHAR(100),
    first_name   VARCHAR(100),
    last_name    VARCHAR(100),
    birthdate    DATE,
    avatar_url   VARCHAR(512),
    location     VARCHAR(100),
    github_url   VARCHAR(512),
    linkedin_url VARCHAR(512),
    twitter_url  VARCHAR(512),
    website_url  VARCHAR(512),
    updated_at   TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT user_profiles_user_id_unique UNIQUE (user_id)
);

-- ── oauth_identities ─────────────────────────────────────────────────────────
CREATE TABLE oauth_identities (
    id               BIGSERIAL    PRIMARY KEY,
    user_id          BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    provider         VARCHAR(20)  NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    linked_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT oauth_identities_provider_user_idx UNIQUE (provider, provider_user_id)
);

CREATE INDEX oauth_identities_user_idx ON oauth_identities (user_id);

-- ── refresh_tokens ───────────────────────────────────────────────────────────
CREATE TABLE refresh_tokens (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  CHAR(64)     NOT NULL,
    issued_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked_at  TIMESTAMPTZ,
    device_hint VARCHAR(255),
    ip_address  VARCHAR(45),
    CONSTRAINT refresh_tokens_hash_idx UNIQUE (token_hash)
);

CREATE INDEX refresh_tokens_user_active_idx ON refresh_tokens (user_id, revoked_at, expires_at);

-- ── email_verification_tokens ────────────────────────────────────────────────
CREATE TABLE email_verification_tokens (
    id            BIGSERIAL    PRIMARY KEY,
    user_id       BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash    CHAR(64)     NOT NULL,
    delivery_mode VARCHAR(20)  NOT NULL,
    expires_at    TIMESTAMPTZ  NOT NULL,
    used_at       TIMESTAMPTZ
);

CREATE INDEX email_verification_tokens_hash_idx ON email_verification_tokens (token_hash);

-- ── pending_email_changes ────────────────────────────────────────────────────
CREATE TABLE pending_email_changes (
    id           BIGSERIAL    PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    new_email    VARCHAR(255) NOT NULL,
    token_hash   CHAR(64)     NOT NULL,
    requested_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at   TIMESTAMPTZ  NOT NULL,
    used_at      TIMESTAMPTZ,
    CONSTRAINT pending_email_changes_user_id_unique UNIQUE (user_id)
);

-- ── password_reset_tokens ────────────────────────────────────────────────────
CREATE TABLE password_reset_tokens (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash CHAR(64)     NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    used_at    TIMESTAMPTZ,
    CONSTRAINT password_reset_tokens_hash_idx UNIQUE (token_hash)
);

-- ── emergency_revoke_tokens ──────────────────────────────────────────────────
CREATE TABLE emergency_revoke_tokens (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash CHAR(64)     NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    used_at    TIMESTAMPTZ,
    CONSTRAINT emergency_revoke_tokens_hash_idx UNIQUE (token_hash)
);

-- ── webauthn_credentials ─────────────────────────────────────────────────────
CREATE TABLE webauthn_credentials (
    id               BIGSERIAL    PRIMARY KEY,
    user_id          BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    credential_id    BYTEA        NOT NULL,
    public_key_cose  BYTEA        NOT NULL,
    sign_count       BIGINT       NOT NULL DEFAULT 0,
    aaguid           VARCHAR(36),
    friendly_name    VARCHAR(100),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT webauthn_credentials_credential_id_idx UNIQUE (credential_id)
);

CREATE INDEX webauthn_credentials_user_idx ON webauthn_credentials (user_id);

-- ── totp_configs ─────────────────────────────────────────────────────────────
CREATE TABLE totp_configs (
    id                   BIGSERIAL    PRIMARY KEY,
    user_id              BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    secret_encrypted     TEXT         NOT NULL,
    enabled              BOOLEAN      NOT NULL DEFAULT FALSE,
    backup_codes_hashes  TEXT         NOT NULL DEFAULT '[]',
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT totp_configs_user_id_unique UNIQUE (user_id)
);

-- ── known_login_ips ──────────────────────────────────────────────────────────
CREATE TABLE known_login_ips (
    id            BIGSERIAL    PRIMARY KEY,
    user_id       BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    ip_hash       CHAR(64)     NOT NULL,
    country_code  CHAR(2),
    city          VARCHAR(100),
    first_seen_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_seen_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT known_login_ips_user_ip_idx UNIQUE (user_id, ip_hash)
);
