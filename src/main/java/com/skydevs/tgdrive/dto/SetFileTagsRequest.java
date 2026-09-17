package com.skydevs.tgdrive.dto;

import lombok.Data;

import java.util.List;

@Data
public class SetFileTagsRequest {
    private List<Long> tagIds;
}
