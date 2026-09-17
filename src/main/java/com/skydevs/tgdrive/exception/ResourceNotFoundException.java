package com.skydevs.tgdrive.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends HttpStatusException {
    public ResourceNotFoundException(String msg) {
        super(HttpStatus.NOT_FOUND, msg);
    }
}
