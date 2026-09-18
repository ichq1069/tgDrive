package com.skydevs.tgdrive.service;

import com.skydevs.tgdrive.entity.ContentLevel;

import java.util.List;

public interface ContentLevelService {
    List<ContentLevel> listAll();
    ContentLevel getById(Long id);
    ContentLevel create(String name, Integer levelOrder, String description);
    ContentLevel update(Long id, String name, Integer levelOrder, String description);
    void delete(Long id);
}
