CREATE TABLE lesson_progress (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES app_user(id),
    lesson_id UUID NOT NULL REFERENCES lesson(id),
    completed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_lesson_progress_student_lesson UNIQUE (student_id, lesson_id)
);

CREATE INDEX idx_lesson_progress_student ON lesson_progress(student_id);
CREATE INDEX idx_lesson_progress_lesson ON lesson_progress(lesson_id);
