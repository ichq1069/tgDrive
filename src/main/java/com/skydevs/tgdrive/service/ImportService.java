package com.skydevs.tgdrive.service;

import java.util.List;

public interface ImportService {
    void importFromUrls(List<String> urls, Long userId, String sourcePage);
    void importFromUrls(List<String> urls, Long userId, String sourcePage, List<String> tags, String contentLevel);
}
