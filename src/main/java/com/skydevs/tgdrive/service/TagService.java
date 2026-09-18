package com.skydevs.tgdrive.service;

import com.skydevs.tgdrive.entity.Tag;

import java.util.List;

public interface TagService {

    List<Tag> listAll();

    List<Tag> listDefault();

    Tag create(String name, Integer priority, Integer isDefault);

    void updateTag(Long id, String name, Integer priority, Integer isDefault);

    void delete(Long id);

    void setFileTags(String fileId, List<Long> tagIds);

    void addFileTags(String fileId, List<Long> tagIds);

    List<Tag> listByFileId(String fileId);
}
