package com.reelvault.api;

import com.reelvault.service.ResourceNotFoundException;
import com.reelvault.service.AccountAlreadyExistsException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import com.reelvault.service.CatalogueProviderException;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(CatalogueProviderException.class)
    public ResponseEntity<Map<String, String>> handleProvider(CatalogueProviderException exception) {
        return ResponseEntity.status(exception.getStatus()).body(Map.of("message", exception.getMessage()));
    }
    @ExceptionHandler(AccountAlreadyExistsException.class)
    ResponseEntity<Map<String, Object>> accountExists() {
        return error(HttpStatus.CONFLICT, "An account with that email already exists.", Map.of());
    }

    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    ResponseEntity<Map<String, Object>> invalidCredentials() {
        return error(HttpStatus.UNAUTHORIZED, "Email or password is incorrect.", Map.of());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<Map<String, Object>> notFound(ResourceNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    ResponseEntity<Map<String, Object>> invalidInput(Exception exception) {
        Map<String, String> details = new LinkedHashMap<>();
        if (exception instanceof MethodArgumentNotValidException validation) {
            for (FieldError error : validation.getBindingResult().getFieldErrors()) {
                details.put(error.getField(), error.getDefaultMessage());
            }
        }
        String message = exception instanceof MethodArgumentNotValidException
                ? "Please correct the highlighted fields." : exception.getMessage();
        return error(HttpStatus.BAD_REQUEST, message, details);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> unexpected(Exception exception) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", Map.of());
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message, Map<String, String> details) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("timestamp", Instant.now());
        response.put("status", status.value());
        response.put("message", message);
        response.put("details", details);
        return ResponseEntity.status(status).body(response);
    }
}
