package com.skydevs.tgdrive.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.skydevs.tgdrive.entity.TagRule;
import com.skydevs.tgdrive.result.Result;
import com.skydevs.tgdrive.service.TagRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/tag-rules")
@RequiredArgsConstructor
public class TagRuleController {

    private final TagRuleService tagRuleService;

    @SaCheckRole("admin")
    @GetMapping
    public Result<List<TagRule>> listAll() {
        return Result.success(tagRuleService.listAll());
    }

    @SaCheckRole("admin")
    @PostMapping
    public Result<TagRule> create(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String ruleType = (String) body.get("ruleType");
        String ruleValue = (String) body.get("ruleValue");
        Long tagId = body.get("tagId") != null ? ((Number) body.get("tagId")).longValue() : null;
        Integer enabled = body.get("enabled") != null ? ((Number) body.get("enabled")).intValue() : 1;
        return Result.success(tagRuleService.create(name, ruleType, ruleValue, tagId, enabled));
    }

    @SaCheckRole("admin")
    @PatchMapping("/{id}")
    public Result<TagRule> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String ruleType = (String) body.get("ruleType");
        String ruleValue = (String) body.get("ruleValue");
        Long tagId = body.get("tagId") != null ? ((Number) body.get("tagId")).longValue() : null;
        Integer enabled = body.get("enabled") != null ? ((Number) body.get("enabled")).intValue() : null;
        return Result.success(tagRuleService.update(id, name, ruleType, ruleValue, tagId, enabled));
    }

    @SaCheckRole("admin")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tagRuleService.delete(id);
        return Result.success();
    }
}
