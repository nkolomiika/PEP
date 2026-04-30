ALTER TABLE validation_job ADD COLUMN dependency_scan_status VARCHAR(32);
ALTER TABLE validation_job ADD COLUMN dependency_scan_summary TEXT;
ALTER TABLE validation_job ADD COLUMN dependency_scan_report TEXT;
ALTER TABLE validation_job ADD COLUMN dependency_scan_finished_at TIMESTAMP WITH TIME ZONE;
