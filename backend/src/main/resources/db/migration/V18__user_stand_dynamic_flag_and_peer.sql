-- Reset legacy user-stand data before switching to per-instance dynamic flags.
DELETE FROM user_pentest_stand_instance;
DELETE FROM user_pentest_stand;

ALTER TABLE user_pentest_stand
    DROP COLUMN flag;

ALTER TABLE user_pentest_stand
    DROP COLUMN solved;

ALTER TABLE user_pentest_stand
    DROP COLUMN solved_at;

ALTER TABLE user_pentest_stand
    ADD COLUMN module_id UUID NOT NULL REFERENCES module(id);

ALTER TABLE user_pentest_stand
    ADD COLUMN review_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT';

ALTER TABLE user_pentest_stand
    ADD COLUMN author_solved BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE user_pentest_stand
    ADD COLUMN author_solved_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE user_pentest_stand
    ADD COLUMN review_comment VARCHAR(500);

ALTER TABLE user_pentest_stand
    ADD COLUMN reviewed_by UUID REFERENCES app_user(id);

ALTER TABLE user_pentest_stand
    ADD COLUMN reviewed_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE user_pentest_stand
    ADD COLUMN submitted_for_review_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_user_pentest_stand_module ON user_pentest_stand(module_id);
CREATE INDEX idx_user_pentest_stand_review_status ON user_pentest_stand(review_status);

ALTER TABLE user_pentest_stand_instance
    ADD COLUMN solver_id UUID REFERENCES app_user(id);

ALTER TABLE user_pentest_stand_instance
    ADD COLUMN flag_hash VARCHAR(64) NOT NULL DEFAULT '';

ALTER TABLE user_pentest_stand_instance
    ADD COLUMN flag_solved BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE user_pentest_stand_instance
    ADD COLUMN flag_solved_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE user_pentest_stand_instance
    ADD COLUMN flag_attempts INTEGER NOT NULL DEFAULT 0;

UPDATE user_pentest_stand_instance SET solver_id = owner_id;

ALTER TABLE user_pentest_stand_instance
    ALTER COLUMN solver_id SET NOT NULL;

CREATE INDEX idx_user_pentest_stand_instance_solver ON user_pentest_stand_instance(solver_id);

CREATE TABLE user_pentest_stand_assignment (
    id UUID PRIMARY KEY,
    stand_id UUID NOT NULL REFERENCES user_pentest_stand(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES app_user(id),
    module_id UUID NOT NULL REFERENCES module(id),
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL,
    solved BOOLEAN NOT NULL DEFAULT FALSE,
    solved_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_user_pentest_stand_assignment UNIQUE (stand_id, user_id)
);

CREATE INDEX idx_user_pentest_stand_assignment_user_module
    ON user_pentest_stand_assignment(user_id, module_id);
CREATE INDEX idx_user_pentest_stand_assignment_stand
    ON user_pentest_stand_assignment(stand_id);
