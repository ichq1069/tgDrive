package com.skydevs.tgdrive.constants;

public final class Libraries {
    public static final String TELE = "tele";
    public static final String SHARED = "shared";
    public static final String PRIVATE = "private";

    private Libraries() {
    }

    public static boolean isValid(String library) {
        return TELE.equals(library) || SHARED.equals(library) || PRIVATE.equals(library);
    }
}
