package com.skydevs.tgdrive.dto;

import lombok.Data;

import java.util.List;

@Data
public class TransferRequest {
    private List<String> fileIds;
    private String targetLibrary;
    private String contentLevel;
}
