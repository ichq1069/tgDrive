package com.skydevs.tgdrive.dto;

import lombok.Data;

import java.util.List;

@Data
public class RestoreRequest {
    private List<String> fileIds;
}
