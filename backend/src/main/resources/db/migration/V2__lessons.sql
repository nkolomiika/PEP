CREATE TABLE lesson (
    id UUID PRIMARY KEY,
    module_id UUID NOT NULL REFERENCES module(id),
    title VARCHAR(200) NOT NULL,
    content_markdown TEXT NOT NULL,
    position INTEGER NOT NULL,
    published BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_lesson_module_position ON lesson(module_id, position);
