package com.itserve.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record ApiError(Instant timestamp, int status, String error, String message, String path,
                           Map<String, String> fieldErrors) {
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage(), request, null);
    }

    @ExceptionHandler(DuplicateUserException.class)
    public ResponseEntity<ApiError> handleDuplicate(DuplicateUserException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "DUPLICATE_USER", ex.getMessage(), request, null);
    }

    @ExceptionHandler(InvalidTicketStatusException.class)
    public ResponseEntity<ApiError> handleInvalidStatus(InvalidTicketStatusException ex, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_TICKET_STATUS", ex.getMessage(), request, null);
    }

    @ExceptionHandler(TicketAlreadyClosedException.class)
    public ResponseEntity<ApiError> handleAlreadyClosed(TicketAlreadyClosedException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "TICKET_ALREADY_CLOSED", ex.getMessage(), request, null);
    }

    @ExceptionHandler(ForbiddenActionException.class)
    public ResponseEntity<ApiError> handleForbiddenAction(ForbiddenActionException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN_ACTION", ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR",
                "Request validation failed", request, fieldErrors);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission to perform this action",
                request, null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Invalid email or password", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Something went wrong. Please try again.", request, null);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String error, String message,
                                           HttpServletRequest request, Map<String, String> fieldErrors) {
        ApiError body = new ApiError(Instant.now(), status.value(), error, message, request.getRequestURI(), fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}
