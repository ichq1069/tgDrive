package com.skydevs.tgdrive.dto;

import lombok.Data;

@Data
public class UpdateMemberLevelRequest {
    private String memberLevel;
    private Boolean confirmDemote;
}
