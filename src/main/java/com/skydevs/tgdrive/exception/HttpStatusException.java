package com.skydevs.tgdrive.exception;

import org.springframework.http.HttpStatus;

public class HttpStatusException extends BaseException {
    private final HttpStatus status;

    public HttpStatusException(HttpStatus status, String msg) {
        super(msg);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
