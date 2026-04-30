ALTER TABLE validation_job ADD COLUMN image_scan_status VARCHAR(32);
ALTER TABLE validation_job ADD COLUMN image_scan_summary TEXT;
ALTER TABLE validation_job ADD COLUMN image_scan_report TEXT;
ALTER TABLE validation_job ADD COLUMN image_scan_finished_at TIMESTAMP WITH TIME ZONE;
