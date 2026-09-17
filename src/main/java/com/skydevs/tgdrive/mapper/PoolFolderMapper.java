package com.skydevs.tgdrive.mapper;

import com.skydevs.tgdrive.entity.PoolFolder;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PoolFolderMapper {

    @Select("SELECT * FROM pool_folders ORDER BY id ASC")
    List<PoolFolder> listAll();

    @Select("SELECT * FROM pool_folders WHERE id = #{id}")
    PoolFolder getById(Long id);

    @Select("SELECT * FROM pool_folders WHERE name = #{name}")
    PoolFolder getByName(String name);

    @Insert("INSERT INTO pool_folders (name) VALUES (#{name})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(PoolFolder folder);

    @Update("UPDATE pool_folders SET name = #{name} WHERE id = #{id}")
    void update(PoolFolder folder);

    @Delete("DELETE FROM pool_folders WHERE id = #{id}")
    void delete(Long id);
}
