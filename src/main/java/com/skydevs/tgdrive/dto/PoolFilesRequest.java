package com.skydevs.tgdrive.dto;

import lombok.Data;

import java.util.List;

@Data
public class PoolFilesRequest {
    private String action;
    private List<String> fileIds;
    private Long folderId;
}
