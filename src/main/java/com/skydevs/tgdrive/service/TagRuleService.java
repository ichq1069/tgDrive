package com.skydevs.tgdrive.service;

import com.skydevs.tgdrive.entity.TagRule;

import java.util.List;

public interface TagRuleService {
    List<TagRule> listAll();
    TagRule getById(Long id);
    TagRule create(String name, String ruleType, String ruleValue, Long tagId, Integer enabled);
    TagRule update(Long id, String name, String ruleType, String ruleValue, Long tagId, Integer enabled);
    void delete(Long id);
    List<Long> matchTagsForFile(String filename);
}
