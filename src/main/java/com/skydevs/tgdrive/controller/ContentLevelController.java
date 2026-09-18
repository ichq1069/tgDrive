package com.skydevs.tgdrive.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.skydevs.tgdrive.entity.ContentLevel;
import com.skydevs.tgdrive.result.Result;
import com.skydevs.tgdrive.service.ContentLevelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ContentLevelController {

    private final ContentLevelService contentLevelService;

    @SaCheckLogin
    @GetMapping("/content-levels")
    public Result<List<ContentLevel>> listAll() {
        return Result.success(contentLevelService.listAll());
    }

    @SaCheckRole("admin")
    @PostMapping("/admin/content-levels")
    public Result<ContentLevel> create(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        Integer levelOrder = body.get("levelOrder") != null ? ((Number) body.get("levelOrder")).intValue() : 0;
        String description = (String) body.get("description");
        return Result.success(contentLevelService.create(name, levelOrder, description));
    }

    @SaCheckRole("admin")
    @PatchMapping("/admin/content-levels/{id}")
    public Result<ContentLevel> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        Integer levelOrder = body.get("levelOrder") != null ? ((Number) body.get("levelOrder")).intValue() : null;
        String description = (String) body.get("description");
        return Result.success(contentLevelService.update(id, name, levelOrder, description));
    }

    @SaCheckRole("admin")
    @DeleteMapping("/admin/content-levels/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        contentLevelService.delete(id);
        return Result.success();
    }
}
