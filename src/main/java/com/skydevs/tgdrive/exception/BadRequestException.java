package com.skydevs.tgdrive.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends HttpStatusException {
    public BadRequestException(String msg) {
        super(HttpStatus.BAD_REQUEST, msg);
    }
}
