package com.skydevs.tgdrive.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends HttpStatusException {
    public UnauthorizedException() {
        super(HttpStatus.UNAUTHORIZED, "请先登录后再访问");
    }

    public UnauthorizedException(String msg) {
        super(HttpStatus.UNAUTHORIZED, msg);
    }
}
