package com.skydevs.tgdrive.dto;

import lombok.Data;

import java.util.List;

@Data
public class UrlImportRequest {
    private List<String> urls;
    private String sourcePage;
}
