package com.skydevs.tgdrive.service;

import com.skydevs.tgdrive.entity.RedeemCode;
import com.skydevs.tgdrive.entity.User;

import java.util.List;

public interface MemberService {

    void updateMemberLevel(Long userId, String memberLevel, boolean confirmDemote);

    String redeem(Long userId, String code);

    List<RedeemCode> listRedeemCodes();

    RedeemCode createRedeemCode(RedeemCode redeemCode);

    void updateRedeemCode(RedeemCode redeemCode);

    void deleteRedeemCode(Long id);
}
