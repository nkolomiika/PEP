ALTER TABLE submission ADD COLUMN source_type VARCHAR(32) NOT NULL DEFAULT 'IMAGE_REFERENCE';
ALTER TABLE submission ADD COLUMN archive_filename VARCHAR(255);
ALTER TABLE submission ADD COLUMN archive_storage_path TEXT;
ALTER TABLE submission ADD COLUMN compose_service VARCHAR(120);
ALTER TABLE submission ADD COLUMN build_context VARCHAR(255);
ALTER TABLE submission ADD COLUMN runtime_image_reference TEXT;
ALTER TABLE submission ADD COLUMN public_url TEXT;
ALTER TABLE submission ADD COLUMN local_host_url TEXT;

UPDATE submission
SET runtime_image_reference = image_reference
WHERE runtime_image_reference IS NULL;
