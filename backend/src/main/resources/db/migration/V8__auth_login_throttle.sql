CREATE TABLE auth_login_throttle (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    remote_address VARCHAR(128) NOT NULL,
    failed_count INTEGER NOT NULL,
    first_failed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    locked_until TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX idx_auth_login_throttle_identity ON auth_login_throttle(email, remote_address);
CREATE INDEX idx_auth_login_throttle_locked_until ON auth_login_throttle(locked_until);
