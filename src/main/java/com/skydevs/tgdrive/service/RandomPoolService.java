package com.skydevs.tgdrive.service;

import com.skydevs.tgdrive.entity.FileInfo;
import com.skydevs.tgdrive.entity.PoolFolder;
import com.skydevs.tgdrive.result.PageResult;

import java.util.List;

public interface RandomPoolService {

    List<PoolFolder> listFolders();

    PoolFolder createFolder(String name);

    void renameFolder(Long id, String name);

    void deleteFolder(Long id);

    void setPoolStatus(List<String> fileIds, boolean inPool, Long folderId);

    FileInfo randomOne(Long folderId, List<Long> tagIds);

    PageResult gallery(String keyword, Long folderId, String fileType, List<Long> tagIds, int page, int size);
}
