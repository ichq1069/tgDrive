package com.skydevs.tgdrive.service;

import com.skydevs.tgdrive.entity.Tag;

import java.util.List;

public interface TagService {

    List<Tag> listAll();

    Tag create(String name);

    void rename(Long id, String name);

    void delete(Long id);

    void setFileTags(String fileId, List<Long> tagIds);

    List<Tag> listByFileId(String fileId);
}
