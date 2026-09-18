package com.skydevs.tgdrive.mapper;

import com.skydevs.tgdrive.entity.TagRule;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TagRuleMapper {

    @Select("SELECT * FROM tag_rules ORDER BY id ASC")
    List<TagRule> listAll();

    @Select("SELECT * FROM tag_rules WHERE enabled = 1")
    List<TagRule> listEnabled();

    @Select("SELECT * FROM tag_rules WHERE id = #{id}")
    TagRule getById(Long id);

    @Select("SELECT * FROM tag_rules WHERE rule_type = #{ruleType} AND enabled = 1")
    List<TagRule> listByType(String ruleType);

    @Insert("INSERT INTO tag_rules (name, rule_type, rule_value, tag_id, enabled) VALUES (#{name}, #{ruleType}, #{ruleValue}, #{tagId}, #{enabled})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(TagRule tagRule);

    @Update("UPDATE tag_rules SET name = #{name}, rule_type = #{ruleType}, rule_value = #{ruleValue}, tag_id = #{tagId}, enabled = #{enabled} WHERE id = #{id}")
    void update(TagRule tagRule);

    @Delete("DELETE FROM tag_rules WHERE id = #{id}")
    void delete(Long id);
}
