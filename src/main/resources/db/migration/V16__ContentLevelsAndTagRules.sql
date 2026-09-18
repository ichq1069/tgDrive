-- Dynamic content levels table
CREATE TABLE content_levels (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL UNIQUE,
  level_order INTEGER NOT NULL DEFAULT 0,
  description TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);

-- Seed default levels
INSERT INTO content_levels (name, level_order, description) VALUES
  ('pt', 0, '普通用户'),
  ('vip', 1, 'VIP会员'),
  ('svip', 2, 'SVIP会员'),
  ('vvip', 3, '超级VIP会员');

-- Tag rules table: auto-apply tags based on conditions
CREATE TABLE tag_rules (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  rule_type TEXT NOT NULL DEFAULT 'extension',
  rule_value TEXT NOT NULL,
  tag_id INTEGER NOT NULL,
  enabled INTEGER NOT NULL DEFAULT 1,
  created_at TEXT DEFAULT (datetime('now')),
  FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

-- Add priority and is_default to tags
ALTER TABLE tags ADD COLUMN priority INTEGER NOT NULL DEFAULT 0;
ALTER TABLE tags ADD COLUMN is_default INTEGER NOT NULL DEFAULT 0;
