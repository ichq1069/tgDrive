package com.skydevs.tgdrive.mapper;

import com.skydevs.tgdrive.entity.ContentLevel;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ContentLevelMapper {

    @Select("SELECT * FROM content_levels ORDER BY level_order ASC")
    List<ContentLevel> listAll();

    @Select("SELECT * FROM content_levels WHERE id = #{id}")
    ContentLevel getById(Long id);

    @Select("SELECT * FROM content_levels WHERE name = #{name}")
    ContentLevel getByName(String name);

    @Insert("INSERT INTO content_levels (name, level_order, description) VALUES (#{name}, #{levelOrder}, #{description})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ContentLevel contentLevel);

    @Update("UPDATE content_levels SET name = #{name}, level_order = #{levelOrder}, description = #{description} WHERE id = #{id}")
    void update(ContentLevel contentLevel);

    @Delete("DELETE FROM content_levels WHERE id = #{id}")
    void delete(Long id);

    @Select("SELECT COUNT(*) FROM content_levels")
    int count();
}
