package com.skydevs.tgdrive.service.impl;

import com.skydevs.tgdrive.constants.MemberLevels;
import com.skydevs.tgdrive.entity.RedeemCode;
import com.skydevs.tgdrive.entity.RedeemRecord;
import com.skydevs.tgdrive.entity.User;
import com.skydevs.tgdrive.exception.BadRequestException;
import com.skydevs.tgdrive.mapper.RedeemCodeMapper;
import com.skydevs.tgdrive.mapper.RedeemRecordMapper;
import com.skydevs.tgdrive.mapper.UserMapper;
import com.skydevs.tgdrive.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final UserMapper userMapper;
    private final RedeemCodeMapper redeemCodeMapper;
    private final RedeemRecordMapper redeemRecordMapper;

    @Override
    public void updateMemberLevel(Long userId, String memberLevel, boolean confirmDemote) {
        if (!MemberLevels.isValid(memberLevel)) {
            throw new BadRequestException("无效的会员等级");
        }
        User user = userMapper.getUserById(userId);
        if (user == null) {
            throw new BadRequestException("用户不存在");
        }
        if (MemberLevels.isLower(memberLevel, user.getMemberLevel()) && !confirmDemote) {
            throw new BadRequestException("降级需要二次确认，请传 confirmDemote=true");
        }
        userMapper.updateMemberLevel(userId, memberLevel);
        log.info("用户 {} 会员等级从 {} 变更为 {}", userId, user.getMemberLevel(), memberLevel);
    }

    @Override
    @Transactional
    public String redeem(Long userId, String code) {
        RedeemCode redeemCode = redeemCodeMapper.getByCode(code);
        if (redeemCode == null) {
            throw new BadRequestException("兑换码不存在");
        }
        if (Boolean.FALSE.equals(redeemCode.getEnabled())) {
            throw new BadRequestException("兑换码已禁用");
        }
        if (redeemCode.getExpiresAt() != null && !redeemCode.getExpiresAt().isEmpty()) {
            LocalDate expiresAt = LocalDate.parse(redeemCode.getExpiresAt());
            if (LocalDate.now().isAfter(expiresAt)) {
                throw new BadRequestException("兑换码已过期");
            }
        }
        if (redeemCode.getUsedCount() >= redeemCode.getMaxUses()) {
            throw new BadRequestException("兑换码已用尽");
        }
        if (redeemRecordMapper.countByCodeAndUser(redeemCode.getId(), userId) > 0) {
            throw new BadRequestException("您已使用过此兑换码");
        }
        User user = userMapper.getUserById(userId);
        if (user == null) {
            throw new BadRequestException("用户不存在");
        }
        String target = redeemCode.getTargetMemberLevel();
        if (!MemberLevels.isHigher(target, user.getMemberLevel())) {
            throw new BadRequestException("兑换码目标等级不高于当前等级，无法升级");
        }
        userMapper.updateMemberLevel(userId, target);
        redeemCodeMapper.incrementUsedCount(redeemCode.getId());
        redeemRecordMapper.insert(RedeemRecord.builder()
                .codeId(redeemCode.getId())
                .userId(userId)
                .redeemedAt(System.currentTimeMillis())
                .build());
        log.info("用户 {} 兑换码 {} 成功，等级升至 {}", userId, code, target);
        return target;
    }

    @Override
    public List<RedeemCode> listRedeemCodes() {
        return redeemCodeMapper.listAll();
    }

    @Override
    public RedeemCode createRedeemCode(RedeemCode redeemCode) {
        if (redeemCode.getCode() == null || redeemCode.getCode().trim().isEmpty()) {
            throw new BadRequestException("兑换码不能为空");
        }
        if (redeemCodeMapper.getByCode(redeemCode.getCode().trim()) != null) {
            throw new BadRequestException("兑换码已存在");
        }
        if (!MemberLevels.isValid(redeemCode.getTargetMemberLevel())) {
            throw new BadRequestException("目标等级无效");
        }
        if (redeemCode.getMaxUses() == null || redeemCode.getMaxUses() < 1) {
            redeemCode.setMaxUses(1);
        }
        redeemCode.setUsedCount(0);
        if (redeemCode.getEnabled() == null) {
            redeemCode.setEnabled(true);
        }
        redeemCodeMapper.insert(redeemCode);
        log.info("创建兑换码: {}", redeemCode.getCode());
        return redeemCode;
    }

    @Override
    public void updateRedeemCode(RedeemCode redeemCode) {
        RedeemCode existing = redeemCodeMapper.getById(redeemCode.getId());
        if (existing == null) {
            throw new BadRequestException("兑换码不存在");
        }
        if (redeemCode.getMaxUses() == null || redeemCode.getMaxUses() < 1) {
            redeemCode.setMaxUses(1);
        }
        redeemCode.setUsedCount(existing.getUsedCount());
        redeemCodeMapper.update(redeemCode);
    }

    @Override
    public void deleteRedeemCode(Long id) {
        RedeemCode existing = redeemCodeMapper.getById(id);
        if (existing == null) {
            throw new BadRequestException("兑换码不存在");
        }
        redeemCodeMapper.delete(id);
    }
}
