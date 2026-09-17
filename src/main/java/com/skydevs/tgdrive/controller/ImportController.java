package com.skydevs.tgdrive.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.skydevs.tgdrive.dto.UrlImportRequest;
import com.skydevs.tgdrive.result.Result;
import com.skydevs.tgdrive.service.ImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;

    @SaCheckLogin
    @PostMapping("/url")
    public Result<String> importFromUrl(@RequestBody UrlImportRequest request) {
        if (request.getUrls() == null || request.getUrls().isEmpty()) {
            return Result.fail("URL列表不能为空");
        }
        // 异步执行导入任务
        Long userId = StpUtil.getLoginIdAsLong();
        importService.importFromUrls(request.getUrls(), userId);
        return Result.success("导入任务已提交，共 " + request.getUrls().size() + " 个文件");
    }
}
