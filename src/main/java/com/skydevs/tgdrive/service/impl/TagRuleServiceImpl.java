package com.skydevs.tgdrive.service.impl;

import com.skydevs.tgdrive.entity.Tag;
import com.skydevs.tgdrive.entity.TagRule;
import com.skydevs.tgdrive.exception.BadRequestException;
import com.skydevs.tgdrive.mapper.TagMapper;
import com.skydevs.tgdrive.mapper.TagRuleMapper;
import com.skydevs.tgdrive.service.TagRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TagRuleServiceImpl implements TagRuleService {

    private final TagRuleMapper tagRuleMapper;
    private final TagMapper tagMapper;

    @Override
    public List<TagRule> listAll() {
        return tagRuleMapper.listAll();
    }

    @Override
    public TagRule getById(Long id) {
        TagRule rule = tagRuleMapper.getById(id);
        if (rule == null) {
            throw new BadRequestException("标签规则不存在");
        }
        return rule;
    }

    @Override
    public TagRule create(String name, String ruleType, String ruleValue, Long tagId, Integer enabled) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("规则名称不能为空");
        }
        if (ruleType == null || ruleType.isBlank()) {
            throw new BadRequestException("规则类型不能为空");
        }
        if (ruleValue == null || ruleValue.isBlank()) {
            throw new BadRequestException("规则值不能为空");
        }
        if (tagId == null) {
            throw new BadRequestException("标签ID不能为空");
        }
        Tag tag = tagMapper.getById(tagId);
        if (tag == null) {
            throw new BadRequestException("标签不存在");
        }
        TagRule rule = TagRule.builder()
                .name(name.trim())
                .ruleType(ruleType.trim())
                .ruleValue(ruleValue.trim())
                .tagId(tagId)
                .enabled(enabled != null ? enabled : 1)
                .build();
        tagRuleMapper.insert(rule);
        return rule;
    }

    @Override
    public TagRule update(Long id, String name, String ruleType, String ruleValue, Long tagId, Integer enabled) {
        TagRule rule = getById(id);
        if (name != null && !name.isBlank()) {
            rule.setName(name.trim());
        }
        if (ruleType != null && !ruleType.isBlank()) {
            rule.setRuleType(ruleType.trim());
        }
        if (ruleValue != null && !ruleValue.isBlank()) {
            rule.setRuleValue(ruleValue.trim());
        }
        if (tagId != null) {
            Tag tag = tagMapper.getById(tagId);
            if (tag == null) {
                throw new BadRequestException("标签不存在");
            }
            rule.setTagId(tagId);
        }
        if (enabled != null) {
            rule.setEnabled(enabled);
        }
        tagRuleMapper.update(rule);
        return rule;
    }

    @Override
    public void delete(Long id) {
        getById(id);
        tagRuleMapper.delete(id);
    }

    @Override
    public List<Long> matchTagsForFile(String filename) {
        List<Long> matchedTagIds = new ArrayList<>();
        if (filename == null || filename.isBlank()) {
            return matchedTagIds;
        }

        List<TagRule> rules = tagRuleMapper.listEnabled();
        String lowerFilename = filename.toLowerCase();

        for (TagRule rule : rules) {
            boolean matched = false;
            switch (rule.getRuleType()) {
                case "extension":
                    String[] exts = rule.getRuleValue().toLowerCase().split(",");
                    for (String ext : exts) {
                        String trimmed = ext.trim();
                        if (!trimmed.isEmpty() && lowerFilename.endsWith(trimmed)) {
                            matched = true;
                            break;
                        }
                    }
                    break;
                case "contains":
                    String[] keywords = rule.getRuleValue().toLowerCase().split(",");
                    for (String keyword : keywords) {
                        String trimmed = keyword.trim();
                        if (!trimmed.isEmpty() && lowerFilename.contains(trimmed)) {
                            matched = true;
                            break;
                        }
                    }
                    break;
                case "regex":
                    try {
                        matched = lowerFilename.matches(rule.getRuleValue());
                    } catch (Exception e) {
                        // skip invalid regex
                    }
                    break;
            }
            if (matched && !matchedTagIds.contains(rule.getTagId())) {
                matchedTagIds.add(rule.getTagId());
            }
        }
        return matchedTagIds;
    }
}
