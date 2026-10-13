package com.reelvault.service;

import org.springframework.http.HttpStatus;

public class CatalogueProviderException extends RuntimeException {
    private final HttpStatus status;
    public CatalogueProviderException(HttpStatus status, String message) { super(message); this.status = status; }
    public HttpStatus getStatus() { return status; }
}
