ALTER TABLE files ADD COLUMN library TEXT NOT NULL DEFAULT 'tele';
ALTER TABLE files ADD COLUMN content_level TEXT;
ALTER TABLE files ADD COLUMN in_random_pool INTEGER NOT NULL DEFAULT 0;
ALTER TABLE files ADD COLUMN pool_folder_id INTEGER;
CREATE INDEX idx_files_library ON files(library);
CREATE INDEX idx_files_random ON files(library, in_random_pool);

UPDATE files SET library = 'shared' WHERE is_public = 1;

ALTER TABLE users ADD COLUMN member_level TEXT NOT NULL DEFAULT 'pt';
UPDATE users SET member_level = 'vvip' WHERE role = 'admin';

CREATE TABLE tags (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL UNIQUE
);

CREATE TABLE file_tags (
  file_id TEXT NOT NULL,
  tag_id INTEGER NOT NULL,
  PRIMARY KEY (file_id, tag_id)
);

CREATE TABLE pool_folders (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL UNIQUE
);

CREATE TABLE private_whitelist (
  user_id INTEGER PRIMARY KEY
);

CREATE TABLE redeem_codes (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  code TEXT NOT NULL UNIQUE,
  code_type TEXT NOT NULL,
  target_member_level TEXT NOT NULL,
  max_uses INTEGER NOT NULL DEFAULT 1,
  used_count INTEGER NOT NULL DEFAULT 0,
  expires_at TEXT,
  enabled INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE redeem_records (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  code_id INTEGER NOT NULL,
  user_id INTEGER NOT NULL,
  redeemed_at INTEGER NOT NULL,
  UNIQUE (code_id, user_id)
);
