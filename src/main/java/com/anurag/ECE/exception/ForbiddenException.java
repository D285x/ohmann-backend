package com.anurag.ECE.exception;

/** The caller is logged in but not allowed to do this (HTTP 403). */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
