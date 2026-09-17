package com.skydevs.tgdrive.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RedeemCode {
    private Long id;
    private String code;
    private String codeType;
    private String targetMemberLevel;
    private Integer maxUses;
    private Integer usedCount;
    private String expiresAt;
    private Boolean enabled;
}
