package com.skydevs.tgdrive.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

public interface ImportService {
    void importFromUrls(List<String> urls, Long userId, String sourcePage, HttpServletRequest request);
    void importFromUrls(List<String> urls, Long userId, String sourcePage, List<String> tags, String contentLevel, HttpServletRequest request);
}
