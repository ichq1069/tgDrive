package com.skydevs.tgdrive.mapper;

import com.skydevs.tgdrive.entity.Tag;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TagMapper {

    @Select("SELECT * FROM tags ORDER BY id ASC")
    List<Tag> listAll();

    @Select("SELECT * FROM tags WHERE id = #{id}")
    Tag getById(Long id);

    @Select("SELECT * FROM tags WHERE name = #{name}")
    Tag getByName(String name);

    @Insert("INSERT INTO tags (name) VALUES (#{name})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Tag tag);

    @Update("UPDATE tags SET name = #{name} WHERE id = #{id}")
    void update(Tag tag);

    @Delete("DELETE FROM tags WHERE id = #{id}")
    void delete(Long id);

    @Insert("INSERT OR IGNORE INTO file_tags (file_id, tag_id) VALUES (#{fileId}, #{tagId})")
    void insertFileTag(@Param("fileId") String fileId, @Param("tagId") Long tagId);

    @Delete("DELETE FROM file_tags WHERE file_id = #{fileId}")
    void deleteFileTags(String fileId);

    @Delete("DELETE FROM file_tags WHERE tag_id = #{tagId}")
    void deleteAssociationsByTagId(Long tagId);

    @Delete("DELETE FROM file_tags WHERE file_id = #{fileId}")
    void deleteAssociationsByFileId(String fileId);

    @Select("SELECT t.* FROM tags t INNER JOIN file_tags ft ON t.id = ft.tag_id WHERE ft.file_id = #{fileId} ORDER BY t.id")
    List<Tag> listByFileId(String fileId);

    @Select("SELECT t.name FROM tags t INNER JOIN file_tags ft ON t.id = ft.tag_id WHERE ft.file_id = #{fileId} ORDER BY t.id")
    List<String> listNamesByFileId(String fileId);
}
