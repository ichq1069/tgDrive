package com.skydevs.tgdrive.dto;

import lombok.Data;

@Data
public class TagNameRequest {
    private String name;
    private Integer priority;
    private Integer isDefault;
}
