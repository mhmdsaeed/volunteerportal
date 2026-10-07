package com.volunteerportal.app.api;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.volunteerportal.app.api.ApiDtos.Error;
import com.volunteerportal.app.service.PasswordService.PasswordRejectedException;

/** JSON errors for the mobile API (the website keeps its HTML error pages). */
@RestControllerAdvice(basePackageClasses = ApiExceptionHandler.class)
public class ApiExceptionHandler {

    private final MessageSource messageSource;

    public ApiExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Error> notFound(EntityNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Error("not_found", e.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Error> forbidden(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new Error("forbidden", e.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Error> badJson(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(new Error("bad_request", "Request body is missing or not valid JSON"));
    }

    @ExceptionHandler(ApiConflictException.class)
    public ResponseEntity<Error> conflict(ApiConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new Error(e.getError(), e.getMessage()));
    }

    /** A refused password change: the app shows its own text for the code, else the server's translated message. */
    @ExceptionHandler(PasswordRejectedException.class)
    public ResponseEntity<Error> passwordRejected(PasswordRejectedException e, HttpServletRequest request) {
        String error = switch (e.getMessageKey()) {
            case "password.error.wrongCurrent" -> "wrong_password";
            case "password.error.tooShort" -> "password_too_short";
            default -> "password_rejected";
        };
        String message = messageSource.getMessage(e.getMessageKey(), null, e.getMessageKey(), ApiLocale.of(request));
        return ResponseEntity.badRequest().body(new Error(error, message));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Error> conflict(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new Error("conflict", e.getMessage()));
    }
}
