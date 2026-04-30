CREATE TABLE report_attachment (
    id UUID PRIMARY KEY,
    report_id UUID NOT NULL REFERENCES report(id),
    original_filename VARCHAR(255) NOT NULL,
    storage_path TEXT NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    size_bytes BIGINT NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_report_attachment_report ON report_attachment(report_id);
