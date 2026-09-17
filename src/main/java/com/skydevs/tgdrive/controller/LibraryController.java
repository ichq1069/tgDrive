package com.skydevs.tgdrive.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.skydevs.tgdrive.dto.*;
import com.skydevs.tgdrive.entity.FileInfo;
import com.skydevs.tgdrive.entity.PoolFolder;
import com.skydevs.tgdrive.result.PageResult;
import com.skydevs.tgdrive.result.Result;
import com.skydevs.tgdrive.service.FileLibraryService;
import com.skydevs.tgdrive.service.RandomPoolService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LibraryController {

    private final FileLibraryService fileLibraryService;
    private final RandomPoolService randomPoolService;

    @SaCheckLogin
    @GetMapping("/libraries/{library}/files")
    public Result<PageResult> listFiles(
            @PathVariable String library,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<Long> tagIds) {
        return Result.success(fileLibraryService.list(library, keyword, tagIds, page, size));
    }

    @SaCheckRole("admin")
    @PostMapping("/libraries/transfer")
    public Result<String> transfer(@RequestBody TransferRequest request) {
        fileLibraryService.transfer(request.getFileIds(), request.getTargetLibrary(), request.getContentLevel());
        return Result.success("转入成功");
    }

    @SaCheckRole("admin")
    @PostMapping("/libraries/restore")
    public Result<String> restore(@RequestBody RestoreRequest request) {
        fileLibraryService.restoreToTele(request.getFileIds());
        return Result.success("转出成功");
    }

    @SaCheckLogin
    @GetMapping("/gallery")
    public Result<PageResult> gallery(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long folderId,
            @RequestParam(required = false) String fileType,
            @RequestParam(required = false) List<Long> tagIds) {
        return Result.success(randomPoolService.gallery(keyword, folderId, fileType, tagIds, page, size));
    }

    @SaCheckLogin
    @GetMapping("/random")
    public Result<FileInfo> random(
            @RequestParam(required = false) Long folderId,
            @RequestParam(required = false) List<Long> tagIds) {
        return Result.success(randomPoolService.randomOne(folderId, tagIds));
    }

    @SaCheckRole("admin")
    @GetMapping("/admin/pool/folders")
    public Result<List<PoolFolder>> listPoolFolders() {
        return Result.success(randomPoolService.listFolders());
    }

    @SaCheckRole("admin")
    @PostMapping("/admin/pool/folders")
    public Result<PoolFolder> createPoolFolder(@RequestBody PoolFolderRequest request) {
        return Result.success(randomPoolService.createFolder(request.getName()));
    }

    @SaCheckRole("admin")
    @PutMapping("/admin/pool/folders/{id}")
    public Result<String> renamePoolFolder(@PathVariable Long id, @RequestBody PoolFolderRequest request) {
        randomPoolService.renameFolder(id, request.getName());
        return Result.success("重命名成功");
    }

    @SaCheckRole("admin")
    @DeleteMapping("/admin/pool/folders/{id}")
    public Result<String> deletePoolFolder(@PathVariable Long id) {
        randomPoolService.deleteFolder(id);
        return Result.success("删除成功");
    }

    @SaCheckRole("admin")
    @PostMapping("/admin/pool/files")
    public Result<String> updatePoolFiles(@RequestBody PoolFilesRequest request) {
        boolean inPool = "add".equals(request.getAction());
        randomPoolService.setPoolStatus(request.getFileIds(), inPool, request.getFolderId());
        return Result.success("操作成功");
    }
}
