package com.hungpc.blog.exception;

import com.hungpc.blog.dto.response.BaseResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<BaseResponse<Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return new ResponseEntity<>(BaseResponse.error(ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<BaseResponse<Object>> handleConflictException(ConflictException ex) {
        return new ResponseEntity<>(BaseResponse.error(ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequestException(BadRequestException ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
        errorResponse.put("status", 400);
        errorResponse.put("error", ex.getErrorCode());
        errorResponse.put("message", ex.getMessage());

        if (ex.getHint() != null) {
            errorResponse.put("hint", ex.getHint());
        }
        if (ex.getSuggestion() != null) {
            errorResponse.put("suggestion", ex.getSuggestion());
        }

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return new ResponseEntity<>(
                BaseResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Validation Failed")
                        .data(errors)
                        .build(),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<BaseResponse<Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return new ResponseEntity<>(BaseResponse.error("Data integrity violation"), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Object>> handleGlobalException(Exception ex) {
        return new ResponseEntity<>(BaseResponse.error("Internal Server Error: " + ex.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
