package com.skydevs.tgdrive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MeProfile {
    private Long userId;
    private String username;
    private String email;
    private String role;
    private String memberLevel;
    private boolean privateAuthorized;
}
