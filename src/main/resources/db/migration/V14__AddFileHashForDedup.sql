-- Add file_hash column for duplicate detection
ALTER TABLE files ADD COLUMN file_hash TEXT;

-- Add unique index on file_hash (allows NULLs for existing records)
CREATE UNIQUE INDEX IF NOT EXISTS idx_files_file_hash ON files(file_hash) WHERE file_hash IS NOT NULL;
