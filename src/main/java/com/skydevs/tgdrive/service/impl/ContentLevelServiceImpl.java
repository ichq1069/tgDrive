package com.skydevs.tgdrive.service.impl;

import com.skydevs.tgdrive.entity.ContentLevel;
import com.skydevs.tgdrive.exception.BadRequestException;
import com.skydevs.tgdrive.mapper.ContentLevelMapper;
import com.skydevs.tgdrive.service.ContentLevelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContentLevelServiceImpl implements ContentLevelService {

    private final ContentLevelMapper contentLevelMapper;

    @Override
    public List<ContentLevel> listAll() {
        return contentLevelMapper.listAll();
    }

    @Override
    public ContentLevel getById(Long id) {
        ContentLevel level = contentLevelMapper.getById(id);
        if (level == null) {
            throw new BadRequestException("权限级别不存在");
        }
        return level;
    }

    @Override
    public ContentLevel create(String name, Integer levelOrder, String description) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("级别名称不能为空");
        }
        ContentLevel existing = contentLevelMapper.getByName(name.trim());
        if (existing != null) {
            throw new BadRequestException("级别名称已存在");
        }
        ContentLevel level = ContentLevel.builder()
                .name(name.trim())
                .levelOrder(levelOrder != null ? levelOrder : 0)
                .description(description)
                .build();
        contentLevelMapper.insert(level);
        return level;
    }

    @Override
    public ContentLevel update(Long id, String name, Integer levelOrder, String description) {
        ContentLevel level = getById(id);
        if (name != null && !name.isBlank()) {
            ContentLevel existing = contentLevelMapper.getByName(name.trim());
            if (existing != null && !existing.getId().equals(id)) {
                throw new BadRequestException("级别名称已存在");
            }
            level.setName(name.trim());
        }
        if (levelOrder != null) {
            level.setLevelOrder(levelOrder);
        }
        if (description != null) {
            level.setDescription(description);
        }
        contentLevelMapper.update(level);
        return level;
    }

    @Override
    public void delete(Long id) {
        getById(id);
        contentLevelMapper.delete(id);
    }
}
