package com.skydevs.tgdrive.service;

import com.skydevs.tgdrive.result.PageResult;

import java.util.List;

public interface FileLibraryService {

    PageResult list(String library, String keyword, List<Long> tagIds, int page, int size);

    void transfer(List<String> fileIds, String targetLibrary, String contentLevel);

    void restoreToTele(List<String> fileIds);
}
