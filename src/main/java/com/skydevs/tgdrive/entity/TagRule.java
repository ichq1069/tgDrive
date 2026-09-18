package com.skydevs.tgdrive.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TagRule {
    private Long id;
    private String name;
    private String ruleType;
    private String ruleValue;
    private Long tagId;
    private Integer enabled;
}
