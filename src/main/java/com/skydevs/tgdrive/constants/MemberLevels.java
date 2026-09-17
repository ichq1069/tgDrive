package com.skydevs.tgdrive.constants;

import java.util.List;

public final class MemberLevels {
    public static final String PT = "pt";
    public static final String VIP = "vip";
    public static final String SVIP = "svip";
    public static final String VVIP = "vvip";

    private static final List<String> ORDER = List.of(PT, VIP, SVIP, VVIP);

    private MemberLevels() {
    }

    public static boolean isValid(String level) {
        return ORDER.contains(level);
    }

    public static int rank(String level) {
        int index = ORDER.indexOf(level);
        return Math.max(index, 0);
    }

    public static boolean isHigher(String candidate, String current) {
        return rank(candidate) > rank(current);
    }

    public static boolean isLower(String candidate, String current) {
        return rank(candidate) < rank(current);
    }
}
