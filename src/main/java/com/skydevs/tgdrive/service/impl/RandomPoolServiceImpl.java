package com.skydevs.tgdrive.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.skydevs.tgdrive.constants.Libraries;
import com.skydevs.tgdrive.entity.FileInfo;
import com.skydevs.tgdrive.entity.PoolFolder;
import com.skydevs.tgdrive.entity.User;
import com.skydevs.tgdrive.exception.BadRequestException;
import com.skydevs.tgdrive.exception.ForbiddenException;
import com.skydevs.tgdrive.exception.ResourceNotFoundException;
import com.skydevs.tgdrive.mapper.FileMapper;
import com.skydevs.tgdrive.mapper.PoolFolderMapper;
import com.skydevs.tgdrive.result.PageResult;
import com.skydevs.tgdrive.service.LibraryAccessService;
import com.skydevs.tgdrive.service.RandomPoolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RandomPoolServiceImpl implements RandomPoolService {

    private final PoolFolderMapper poolFolderMapper;
    private final FileMapper fileMapper;
    private final LibraryAccessService libraryAccessService;

    @Override
    public List<PoolFolder> listFolders() {
        return poolFolderMapper.listAll();
    }

    @Override
    public PoolFolder createFolder(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BadRequestException("文件夹名不能为空");
        }
        String trimmed = name.trim();
        if (poolFolderMapper.getByName(trimmed) != null) {
            throw new BadRequestException("文件夹名已存在");
        }
        PoolFolder folder = PoolFolder.builder().name(trimmed).build();
        poolFolderMapper.insert(folder);
        log.info("创建随机池文件夹: {}", trimmed);
        return folder;
    }

    @Override
    public void renameFolder(Long id, String name) {
        PoolFolder folder = poolFolderMapper.getById(id);
        if (folder == null) {
            throw new BadRequestException("文件夹不存在");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new BadRequestException("文件夹名不能为空");
        }
        String trimmed = name.trim();
        PoolFolder existing = poolFolderMapper.getByName(trimmed);
        if (existing != null && !existing.getId().equals(id)) {
            throw new BadRequestException("文件夹名已存在");
        }
        folder.setName(trimmed);
        poolFolderMapper.update(folder);
    }

    @Override
    @Transactional
    public void deleteFolder(Long id) {
        PoolFolder folder = poolFolderMapper.getById(id);
        if (folder == null) {
            throw new BadRequestException("文件夹不存在");
        }
        fileMapper.clearPoolFolder(id);
        poolFolderMapper.delete(id);
        log.info("删除随机池文件夹: {}", folder.getName());
    }

    @Override
    @Transactional
    public void setPoolStatus(List<String> fileIds, boolean inPool, Long folderId) {
        User user = libraryAccessService.requireCurrentUser();
        if (!"admin".equals(user.getRole())) {
            throw new ForbiddenException("仅管理员可操作随机池");
        }
        for (String fileId : fileIds) {
            FileInfo file = fileMapper.getFileByFileId(fileId);
            if (file == null) {
                throw new BadRequestException("文件不存在: " + fileId);
            }
            if (!Libraries.SHARED.equals(file.getLibrary())) {
                throw new BadRequestException("仅共享库文件可加入随机池");
            }
            Long targetFolderId = null;
            if (inPool) {
                targetFolderId = folderId;
            }
            fileMapper.updateRandomPool(fileId, inPool, targetFolderId);
        }
    }

    @Override
    public FileInfo randomOne(Long folderId, List<Long> tagIds) {
        List<FileInfo> pool = fileMapper.getRandomPoolFiles(folderId, tagIds);
        if (pool.isEmpty()) {
            throw new ResourceNotFoundException("随机池暂无内容");
        }
        int index = (int) (Math.random() * pool.size());
        return pool.get(index);
    }

    @Override
    public PageResult gallery(String keyword, Long folderId, String fileType, List<Long> tagIds, int page, int size) {
        libraryAccessService.requireCurrentUser();
        PageHelper.startPage(page, size);
        List<FileInfo> files = fileMapper.getGalleryFiles(keyword, folderId, fileType, tagIds);
        PageInfo<FileInfo> pageInfo = new PageInfo<>(files);
        return new PageResult((int) pageInfo.getTotal(), pageInfo.getList());
    }
}
