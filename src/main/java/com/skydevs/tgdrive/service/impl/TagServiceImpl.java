package com.skydevs.tgdrive.service.impl;

import com.skydevs.tgdrive.entity.FileInfo;
import com.skydevs.tgdrive.entity.Tag;
import com.skydevs.tgdrive.entity.User;
import com.skydevs.tgdrive.exception.BadRequestException;
import com.skydevs.tgdrive.exception.ForbiddenException;
import com.skydevs.tgdrive.mapper.FileMapper;
import com.skydevs.tgdrive.mapper.TagMapper;
import com.skydevs.tgdrive.service.LibraryAccessService;
import com.skydevs.tgdrive.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;
    private final FileMapper fileMapper;
    private final LibraryAccessService libraryAccessService;

    @Override
    public List<Tag> listAll() {
        return tagMapper.listAll();
    }

    @Override
    public List<Tag> listDefault() {
        return tagMapper.listDefault();
    }

    @Override
    public Tag create(String name, Integer priority, Integer isDefault) {
        if (name == null || name.trim().isEmpty()) {
            throw new BadRequestException("标签名不能为空");
        }
        String trimmed = name.trim();
        if (tagMapper.getByName(trimmed) != null) {
            throw new BadRequestException("标签已存在");
        }
        Tag tag = Tag.builder()
                .name(trimmed)
                .priority(priority != null ? priority : 0)
                .isDefault(isDefault != null ? isDefault : 0)
                .build();
        tagMapper.insert(tag);
        log.info("创建标签: {}", trimmed);
        return tag;
    }

    @Override
    public void updateTag(Long id, String name, Integer priority, Integer isDefault) {
        Tag tag = tagMapper.getById(id);
        if (tag == null) {
            throw new BadRequestException("标签不存在");
        }
        if (name != null && !name.trim().isEmpty()) {
            String trimmed = name.trim();
            Tag existing = tagMapper.getByName(trimmed);
            if (existing != null && !existing.getId().equals(id)) {
                throw new BadRequestException("标签名已存在");
            }
            tag.setName(trimmed);
        }
        if (priority != null) {
            tag.setPriority(priority);
        }
        if (isDefault != null) {
            tag.setIsDefault(isDefault);
        }
        tagMapper.update(tag);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Tag tag = tagMapper.getById(id);
        if (tag == null) {
            throw new BadRequestException("标签不存在");
        }
        tagMapper.deleteAssociationsByTagId(id);
        tagMapper.delete(id);
        log.info("删除标签: {}", tag.getName());
    }

    @Override
    @Transactional
    public void setFileTags(String fileId, List<Long> tagIds) {
        User user = libraryAccessService.requireCurrentUser();
        FileInfo file = fileMapper.getFileByFileId(fileId);
        if (file == null) {
            throw new BadRequestException("文件不存在");
        }
        boolean isOwner = file.getUserId() != null && file.getUserId().equals(user.getId());
        boolean isAdmin = "admin".equals(user.getRole());
        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("无权设置此文件标签");
        }
        tagMapper.deleteAssociationsByFileId(fileId);
        if (tagIds != null) {
            for (Long tagId : tagIds) {
                tagMapper.insertFileTag(fileId, tagId);
            }
        }
    }

    @Override
    public void addFileTags(String fileId, List<Long> tagIds) {
        if (tagIds != null) {
            for (Long tagId : tagIds) {
                tagMapper.insertFileTag(fileId, tagId);
            }
        }
    }

    @Override
    public List<Tag> listByFileId(String fileId) {
        return tagMapper.listByFileId(fileId);
    }

    @Override
    public Tag findByName(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        return tagMapper.getByName(name.trim());
    }
}
