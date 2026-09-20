package com.skydevs.tgdrive.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.skydevs.tgdrive.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/scrape")
@RequiredArgsConstructor
public class ScrapeController {

    private final JdbcTemplate jdbcTemplate;

    @SaCheckLogin
    @GetMapping("/rule-groups")
    public Result<Map<String, Object>> getRuleGroups() {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM scrape_rule_groups ORDER BY id");
            List<Map<String, Object>> groups = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                Map<String, Object> g = new HashMap<>();
                g.put("key", row.get("rule_key"));
                g.put("name", row.get("name"));
                g.put("kw", row.get("kw"));
                g.put("ext", row.get("ext"));
                g.put("mb", row.get("mb"));
                g.put("must", row.get("must"));
                g.put("defaultLevel", row.get("default_level"));
                g.put("defaultTags", row.get("default_tags"));
                g.put("tagRegex", row.get("tag_regex"));
                g.put("cssSelector", row.get("css_selector"));
                groups.add(g);
            }
            Map<String, Object> data = new HashMap<>();
            data.put("groups", groups);
            return Result.success(data);
        } catch (Exception e) {
            log.warn("获取规则组失败: {}", e.getMessage());
            Map<String, Object> data = new HashMap<>();
            data.put("groups", Collections.emptyList());
            return Result.success(data);
        }
    }

    @SaCheckLogin
    @PostMapping("/rule-groups")
    public Result<Void> saveRuleGroups(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> groups = (List<Map<String, Object>>) body.get("groups");
        if (groups == null) groups = Collections.emptyList();

        try {
            jdbcTemplate.execute("DELETE FROM scrape_rule_groups");
            for (Map<String, Object> g : groups) {
                jdbcTemplate.update(
                    "INSERT INTO scrape_rule_groups (rule_key, name, kw, ext, mb, must, default_level, default_tags, tag_regex, css_selector) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    g.get("key"), g.get("name"), g.get("kw"), g.get("ext"),
                    g.get("mb"), g.get("must"), g.get("defaultLevel"),
                    g.get("defaultTags"), g.get("tagRegex"), g.get("cssSelector"));
            }
            return Result.success();
        } catch (Exception e) {
            log.error("保存规则组失败", e);
            return Result.error("保存失败: " + e.getMessage());
        }
    }
}
