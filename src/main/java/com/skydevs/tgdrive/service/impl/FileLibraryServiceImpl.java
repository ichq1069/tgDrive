package com.skydevs.tgdrive.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.skydevs.tgdrive.constants.Libraries;
import com.skydevs.tgdrive.entity.FileInfo;
import com.skydevs.tgdrive.entity.User;
import com.skydevs.tgdrive.exception.BadRequestException;
import com.skydevs.tgdrive.exception.ForbiddenException;
import com.skydevs.tgdrive.mapper.FileMapper;
import com.skydevs.tgdrive.mapper.TagMapper;
import com.skydevs.tgdrive.result.PageResult;
import com.skydevs.tgdrive.service.FileLibraryService;
import com.skydevs.tgdrive.service.LibraryAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileLibraryServiceImpl implements FileLibraryService {

    private final FileMapper fileMapper;
    private final LibraryAccessService libraryAccessService;
    private final TagMapper tagMapper;

    @Override
    public PageResult list(String library, String keyword, List<Long> tagIds, int page, int size) {
        User user = libraryAccessService.requireCurrentUser();
        if (!libraryAccessService.canList(library, user)) {
            throw new ForbiddenException("无权访问该库");
        }
        PageHelper.startPage(page, size);
        List<FileInfo> files = fileMapper.getLibraryFiles(library, keyword, user.getId(), user.getRole(), tagIds);
        PageInfo<FileInfo> pageInfo = new PageInfo<>(files);
        return new PageResult((int) pageInfo.getTotal(), pageInfo.getList());
    }

    @Override
    @Transactional
    public void transfer(List<String> fileIds, String targetLibrary, String contentLevel) {
        User user = libraryAccessService.requireCurrentUser();
        if (!"admin".equals(user.getRole())) {
            throw new ForbiddenException("仅管理员可执行转入");
        }
        if (!Libraries.SHARED.equals(targetLibrary) && !Libraries.PRIVATE.equals(targetLibrary)) {
            throw new BadRequestException("转入目标必须是 shared 或 private");
        }
        for (String fileId : fileIds) {
            FileInfo file = fileMapper.getFileByFileId(fileId);
            if (file == null) {
                throw new BadRequestException("文件不存在: " + fileId);
            }
            if (!Libraries.TELE.equals(file.getLibrary())) {
                throw new BadRequestException("只能从 Tele 库转出文件");
            }
            String newContentLevel = contentLevel;
            boolean inRandomPool = false;
            Long poolFolderId = null;
            if (Libraries.PRIVATE.equals(targetLibrary)) {
                if (contentLevel == null || contentLevel.isEmpty()) {
                    throw new BadRequestException("转入私密库必须指定 content_level");
                }
                if (!"pt".equals(contentLevel) && !"vip".equals(contentLevel) && !"svip".equals(contentLevel) && !"vvip".equals(contentLevel)) {
                    throw new BadRequestException("content_level 必须是 pt/vip/svip/vvvip");
                }
            }
            boolean newIsPublic = Libraries.SHARED.equals(targetLibrary) || file.isPublic();
            fileMapper.updateLibrary(fileId, targetLibrary, newIsPublic, newContentLevel, inRandomPool, poolFolderId);
            log.info("文件 {} 从 {} 转入 {} by admin {}", fileId, file.getLibrary(), targetLibrary, user.getId());
        }
    }

    @Override
    @Transactional
    public void restoreToTele(List<String> fileIds) {
        User user = libraryAccessService.requireCurrentUser();
        if (!"admin".equals(user.getRole())) {
            throw new ForbiddenException("仅管理员可执行转出");
        }
        for (String fileId : fileIds) {
            FileInfo file = fileMapper.getFileByFileId(fileId);
            if (file == null) {
                throw new BadRequestException("文件不存在: " + fileId);
            }
            if (!Libraries.SHARED.equals(file.getLibrary()) && !Libraries.PRIVATE.equals(file.getLibrary())) {
                throw new BadRequestException("只能从共享库或私密库转回 Tele 库");
            }
            fileMapper.updateLibrary(fileId, Libraries.TELE, false, null, false, null);
            log.info("文件 {} 从 {} 转回 Tele 库 by admin {}", fileId, file.getLibrary(), user.getId());
        }
    }
}
