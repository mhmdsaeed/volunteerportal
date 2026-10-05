package com.volunteerportal.app.api;

/**
 * A request the API refuses because of the current state (409), with an error code the app can translate,
 * e.g. "already_requested".
 */
public class ApiConflictException extends RuntimeException {

    private final String error;

    public ApiConflictException(String error, String message) {
        super(message);
        this.error = error;
    }

    public String getError() {
        return error;
    }
}
