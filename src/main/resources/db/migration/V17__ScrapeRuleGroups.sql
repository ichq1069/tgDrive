CREATE TABLE IF NOT EXISTS scrape_rule_groups (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    rule_key TEXT NOT NULL DEFAULT '*',
    name TEXT,
    kw TEXT DEFAULT '',
    ext TEXT DEFAULT 'svg',
    mb INTEGER DEFAULT 10,
    must TEXT DEFAULT '',
    default_level TEXT DEFAULT '',
    default_tags TEXT DEFAULT '',
    tag_regex TEXT DEFAULT '',
    created_at TEXT DEFAULT (datetime('now')),
    updated_at TEXT DEFAULT (datetime('now'))
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_scrape_rule_groups_key ON scrape_rule_groups(rule_key);
