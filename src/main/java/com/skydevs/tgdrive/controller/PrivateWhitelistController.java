package com.skydevs.tgdrive.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.skydevs.tgdrive.dto.WhitelistRequest;
import com.skydevs.tgdrive.result.Result;
import com.skydevs.tgdrive.service.PrivateWhitelistService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/private-whitelist")
@SaCheckRole("admin")
@RequiredArgsConstructor
public class PrivateWhitelistController {

    private final PrivateWhitelistService whitelistService;

    @GetMapping
    public Result<List<Long>> listWhitelist() {
        return Result.success(whitelistService.listUserIds());
    }

    @PostMapping
    public Result<String> addWhitelist(@RequestBody WhitelistRequest request) {
        whitelistService.add(request.getUserId());
        return Result.success("加入白名单成功");
    }

    @DeleteMapping
    public Result<String> removeWhitelist(@RequestBody WhitelistRequest request) {
        whitelistService.remove(request.getUserId());
        return Result.success("移除白名单成功");
    }
}
