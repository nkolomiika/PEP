CREATE TABLE student_stream (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL UNIQUE,
    description TEXT,
    status VARCHAR(32) NOT NULL,
    created_by UUID REFERENCES app_user(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_student_stream_status ON student_stream(status);

CREATE TABLE student_stream_course (
    id UUID PRIMARY KEY,
    stream_id UUID NOT NULL REFERENCES student_stream(id) ON DELETE CASCADE,
    course_id UUID NOT NULL REFERENCES course(id),
    position INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_student_stream_course UNIQUE (stream_id, course_id)
);

CREATE INDEX idx_student_stream_course_stream ON student_stream_course(stream_id);
CREATE INDEX idx_student_stream_course_course ON student_stream_course(course_id);

CREATE TABLE student_stream_module_schedule (
    id UUID PRIMARY KEY,
    stream_id UUID NOT NULL REFERENCES student_stream(id) ON DELETE CASCADE,
    module_id UUID NOT NULL REFERENCES module(id),
    starts_at TIMESTAMP WITH TIME ZONE,
    submission_deadline TIMESTAMP WITH TIME ZONE,
    black_box_starts_at TIMESTAMP WITH TIME ZONE,
    black_box_deadline TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_student_stream_module_schedule UNIQUE (stream_id, module_id)
);

CREATE INDEX idx_student_stream_module_schedule_stream
    ON student_stream_module_schedule(stream_id);

CREATE TABLE student_stream_member (
    id UUID PRIMARY KEY,
    stream_id UUID NOT NULL REFERENCES student_stream(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES app_user(id),
    status VARCHAR(32) NOT NULL,
    enrolled_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_student_stream_member UNIQUE (stream_id, user_id)
);

CREATE INDEX idx_student_stream_member_stream ON student_stream_member(stream_id);
CREATE INDEX idx_student_stream_member_user ON student_stream_member(user_id);
