package com.inventory.inventorymanagementsystem.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProductNotFoundException(
            ProductNotFoundException exception) {

        log.warn("Product not found exception: {}", exception.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message(exception.getMessage())
                .timestamp(LocalDateTime.now())
                .path(ServletUriComponentsBuilder.fromCurrentRequest().toUriString())
                .build();

        log.debug("Responding with NOT_FOUND status for: {}", errorResponse.getPath());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {
        log.error("Unhandled exception occurred", ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message(ex.getMessage() != null ? ex.getMessage() : "An internal server error occurred")
                .timestamp(LocalDateTime.now())
                .path(ServletUriComponentsBuilder.fromCurrentRequest().toUriString())
                .build();

        log.debug("Responding with INTERNAL_SERVER_ERROR status for: {}", errorResponse.getPath());
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(
            MethodArgumentNotValidException exception) {

        log.warn("Validation error occurred: {}", exception.getBindingResult().getErrorCount() + " field(s) failed validation");

        Map<String, String> errors = new HashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error -> {
                    log.debug("Validation error - Field: {}, Message: {}", error.getField(), error.getDefaultMessage());
                    errors.put(error.getField(), error.getDefaultMessage());
                });

        log.debug("Responding with BAD_REQUEST status with {} validation errors", errors.size());
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }
}
