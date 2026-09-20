package com.skydevs.tgdrive.dto;

import lombok.Data;

import java.util.List;

@Data
public class UrlImportRequest {
    private List<String> urls;
    private String sourcePage;
    private List<String> tags;
    private String contentLevel;
    private String cookie;
}
