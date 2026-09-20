-- V18: Add css_selector to scrape_rule_groups for sniffing rules
ALTER TABLE scrape_rule_groups ADD COLUMN css_selector TEXT DEFAULT '';
