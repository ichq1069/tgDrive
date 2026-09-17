package com.skydevs.tgdrive.mapper;

import com.skydevs.tgdrive.entity.RedeemCode;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RedeemCodeMapper {

    @Select("SELECT * FROM redeem_codes ORDER BY id DESC")
    List<RedeemCode> listAll();

    @Select("SELECT * FROM redeem_codes WHERE id = #{id}")
    RedeemCode getById(Long id);

    @Select("SELECT * FROM redeem_codes WHERE code = #{code}")
    RedeemCode getByCode(String code);

    @Insert("INSERT INTO redeem_codes (code, code_type, target_member_level, max_uses, used_count, expires_at, enabled) VALUES (#{code}, #{codeType}, #{targetMemberLevel}, #{maxUses}, #{usedCount}, #{expiresAt}, #{enabled})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(RedeemCode redeemCode);

    @Update("UPDATE redeem_codes SET code = #{code}, code_type = #{codeType}, target_member_level = #{targetMemberLevel}, max_uses = #{maxUses}, expires_at = #{expiresAt}, enabled = #{enabled} WHERE id = #{id}")
    void update(RedeemCode redeemCode);

    @Update("UPDATE redeem_codes SET used_count = used_count + 1 WHERE id = #{id}")
    void incrementUsedCount(Long id);

    @Delete("DELETE FROM redeem_codes WHERE id = #{id}")
    void delete(Long id);
}
