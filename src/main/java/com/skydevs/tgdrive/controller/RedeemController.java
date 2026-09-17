package com.skydevs.tgdrive.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.skydevs.tgdrive.dto.RedeemCodeRequest;
import com.skydevs.tgdrive.dto.RedeemRequest;
import com.skydevs.tgdrive.entity.RedeemCode;
import com.skydevs.tgdrive.result.Result;
import com.skydevs.tgdrive.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RedeemController {

    private final MemberService memberService;

    @SaCheckLogin
    @PostMapping("/auth/redeem")
    public Result<String> redeem(@RequestBody RedeemRequest request) {
        long userId = cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
        String targetLevel = memberService.redeem(userId, request.getCode());
        return Result.success("兑换成功，等级升至 " + targetLevel);
    }

    @SaCheckRole("admin")
    @GetMapping("/admin/redeem-codes")
    public Result<List<RedeemCode>> listCodes() {
        return Result.success(memberService.listRedeemCodes());
    }

    @SaCheckRole("admin")
    @PostMapping("/admin/redeem-codes")
    public Result<RedeemCode> createCode(@RequestBody RedeemCodeRequest request) {
        RedeemCode code = RedeemCode.builder()
                .code(request.getCode())
                .codeType(request.getCodeType())
                .targetMemberLevel(request.getTargetMemberLevel())
                .maxUses(request.getMaxUses())
                .expiresAt(request.getExpiresAt())
                .enabled(request.getEnabled())
                .build();
        return Result.success(memberService.createRedeemCode(code));
    }

    @SaCheckRole("admin")
    @PatchMapping("/admin/redeem-codes/{id}")
    public Result<String> updateCode(@PathVariable Long id, @RequestBody RedeemCodeRequest request) {
        RedeemCode code = RedeemCode.builder()
                .id(id)
                .code(request.getCode())
                .codeType(request.getCodeType())
                .targetMemberLevel(request.getTargetMemberLevel())
                .maxUses(request.getMaxUses())
                .expiresAt(request.getExpiresAt())
                .enabled(request.getEnabled())
                .build();
        memberService.updateRedeemCode(code);
        return Result.success("更新成功");
    }

    @SaCheckRole("admin")
    @DeleteMapping("/admin/redeem-codes/{id}")
    public Result<String> deleteCode(@PathVariable Long id) {
        memberService.deleteRedeemCode(id);
        return Result.success("删除成功");
    }
}
