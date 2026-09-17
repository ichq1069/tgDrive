package com.skydevs.tgdrive.mapper;

import com.skydevs.tgdrive.entity.RedeemRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RedeemRecordMapper {

    @Select("SELECT COUNT(1) FROM redeem_records WHERE code_id = #{codeId} AND user_id = #{userId}")
    int countByCodeAndUser(Long codeId, Long userId);

    @Insert("INSERT INTO redeem_records (code_id, user_id, redeemed_at) VALUES (#{codeId}, #{userId}, #{redeemedAt})")
    void insert(RedeemRecord record);
}
