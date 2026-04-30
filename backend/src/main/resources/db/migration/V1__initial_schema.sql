CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(160) NOT NULL,
    role VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE course (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE module (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL REFERENCES course(id),
    title VARCHAR(200) NOT NULL,
    vulnerability_topic VARCHAR(120) NOT NULL,
    starts_at TIMESTAMP WITH TIME ZONE,
    submission_deadline TIMESTAMP WITH TIME ZONE,
    black_box_starts_at TIMESTAMP WITH TIME ZONE,
    black_box_deadline TIMESTAMP WITH TIME ZONE,
    status VARCHAR(32) NOT NULL
);

CREATE TABLE submission (
    id UUID PRIMARY KEY,
    module_id UUID NOT NULL REFERENCES module(id),
    student_id UUID NOT NULL REFERENCES app_user(id),
    image_reference TEXT NOT NULL,
    application_port INTEGER NOT NULL,
    health_path VARCHAR(255),
    status VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE,
    approved_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE validation_job (
    id UUID PRIMARY KEY,
    submission_id UUID NOT NULL REFERENCES submission(id),
    image_reference TEXT NOT NULL,
    status VARCHAR(64) NOT NULL,
    logs_uri TEXT,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    finished_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE lab_instance (
    id UUID PRIMARY KEY,
    submission_id UUID NOT NULL REFERENCES submission(id),
    namespace VARCHAR(120) NOT NULL,
    deployment_name VARCHAR(120) NOT NULL,
    service_name VARCHAR(120) NOT NULL,
    route_url TEXT,
    status VARCHAR(32) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE black_box_assignment (
    id UUID PRIMARY KEY,
    module_id UUID NOT NULL REFERENCES module(id),
    student_id UUID NOT NULL REFERENCES app_user(id),
    target_lab_instance_id UUID NOT NULL REFERENCES lab_instance(id),
    status VARCHAR(32) NOT NULL,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE report (
    id UUID PRIMARY KEY,
    author_id UUID NOT NULL REFERENCES app_user(id),
    module_id UUID NOT NULL REFERENCES module(id),
    submission_id UUID REFERENCES submission(id),
    black_box_assignment_id UUID REFERENCES black_box_assignment(id),
    type VARCHAR(32) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content_markdown TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE review (
    id UUID PRIMARY KEY,
    report_id UUID NOT NULL REFERENCES report(id),
    curator_id UUID NOT NULL REFERENCES app_user(id),
    decision VARCHAR(32) NOT NULL,
    score INTEGER NOT NULL,
    comment_markdown TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE audit_event (
    id UUID PRIMARY KEY,
    actor_id UUID REFERENCES app_user(id),
    action VARCHAR(120) NOT NULL,
    target_type VARCHAR(80) NOT NULL,
    target_id UUID,
    metadata_json TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
