package com.skydevs.tgdrive.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WhitelistMapper {

    @Select("SELECT user_id FROM private_whitelist ORDER BY user_id")
    List<Long> listUserIds();

    @Select("SELECT COUNT(1) FROM private_whitelist WHERE user_id = #{userId}")
    int countByUserId(Long userId);

    @Insert("INSERT OR IGNORE INTO private_whitelist (user_id) VALUES (#{userId})")
    void insert(Long userId);

    @Delete("DELETE FROM private_whitelist WHERE user_id = #{userId}")
    void delete(Long userId);
}
