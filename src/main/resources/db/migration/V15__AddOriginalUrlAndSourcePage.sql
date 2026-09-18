-- Add original_url and source_page columns for traceability
ALTER TABLE files ADD COLUMN original_url TEXT;
ALTER TABLE files ADD COLUMN source_page TEXT;
