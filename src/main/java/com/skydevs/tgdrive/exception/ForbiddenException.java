package com.skydevs.tgdrive.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends HttpStatusException {
    public ForbiddenException() {
        super(HttpStatus.FORBIDDEN, "权限不足");
    }

    public ForbiddenException(String msg) {
        super(HttpStatus.FORBIDDEN, msg);
    }
}
