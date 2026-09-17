package com.skydevs.tgdrive.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.skydevs.tgdrive.dto.SetFileTagsRequest;
import com.skydevs.tgdrive.dto.TagNameRequest;
import com.skydevs.tgdrive.entity.Tag;
import com.skydevs.tgdrive.result.Result;
import com.skydevs.tgdrive.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @SaCheckRole("admin")
    @GetMapping("/admin/tags")
    public Result<List<Tag>> listTags() {
        return Result.success(tagService.listAll());
    }

    @SaCheckRole("admin")
    @PostMapping("/admin/tags")
    public Result<Tag> createTag(@RequestBody TagNameRequest request) {
        return Result.success(tagService.create(request.getName()));
    }

    @SaCheckRole("admin")
    @PatchMapping("/admin/tags/{id}")
    public Result<String> renameTag(@PathVariable Long id, @RequestBody TagNameRequest request) {
        tagService.rename(id, request.getName());
        return Result.success("重命名成功");
    }

    @SaCheckRole("admin")
    @DeleteMapping("/admin/tags/{id}")
    public Result<String> deleteTag(@PathVariable Long id) {
        tagService.delete(id);
        return Result.success("删除成功");
    }

    @SaCheckLogin
    @PostMapping("/files/{fileId}/tags")
    public Result<String> setFileTags(@PathVariable String fileId, @RequestBody SetFileTagsRequest request) {
        tagService.setFileTags(fileId, request.getTagIds());
        return Result.success("标签设置成功");
    }

    @SaCheckLogin
    @GetMapping("/files/{fileId}/tags")
    public Result<List<Tag>> getFileTags(@PathVariable String fileId) {
        return Result.success(tagService.listByFileId(fileId));
    }
}
