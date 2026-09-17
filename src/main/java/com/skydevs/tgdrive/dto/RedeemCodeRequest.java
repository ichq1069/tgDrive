package com.skydevs.tgdrive.dto;

import lombok.Data;

@Data
public class RedeemCodeRequest {
    private String code;
    private String codeType;
    private String targetMemberLevel;
    private Integer maxUses;
    private String expiresAt;
    private Boolean enabled;
}
