package com.skydevs.tgdrive.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RedeemRecord {
    private Long id;
    private Long codeId;
    private Long userId;
    private Long redeemedAt;
}
