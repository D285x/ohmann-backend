package com.anurag.ECE.aero;

/** The aero-service (shockFLOW wrapper) could not be reached, or returned an error. */
public class AeroServiceException extends RuntimeException {
    public AeroServiceException(String message) {
        super(message);
    }

    public AeroServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
