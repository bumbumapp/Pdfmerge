package com.bumbumapps;

public enum PermissionReasons {
    ON_CREATE(700),
    MERGE_FILES(701),
    START_CAMERA(702),
    START_HTML(703),
    START_TEXT(704);

    private final int code;

    PermissionReasons(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
