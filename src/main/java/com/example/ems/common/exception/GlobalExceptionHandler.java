package com.example.ems.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 404 - Resource not found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request){

        return buildResponse(HttpStatus.NOT_FOUND,ex.getMessage(), request, null);
    }

    // 400 - Invalid Input
    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInput(InvalidInputException ex, HttpServletRequest request){

        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    // 400 - Validation Errors (@Valid DTO/Controller)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request){

        // building errors map
        Map<String, String> fieldErrors = new HashMap<>();
        for(FieldError fieldError : ex.getBindingResult().getFieldErrors()){
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed: " + fieldErrors.size() + " error(s)", request, fieldErrors);
    }

    // 409 - Duplicate Resource
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException ex, HttpServletRequest request){

        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    // 400 - Malformed/unreadable JSON body
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request){

        return buildResponse(HttpStatus.BAD_REQUEST, "Malformed request body", request, null);
    }

    // 409 - Database Constraint Violation
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request){

        String msg = "Database Constraint Violation";
        if(ex.getRootCause() != null){
            msg += ": " + ex.getRootCause().getMessage();
        }
        return buildResponse(HttpStatus.CONFLICT, msg, request, null);
    }

    // 403 - Method security denial (@PreAuthorize): authenticated but insufficient role
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request){

        return buildResponse(HttpStatus.FORBIDDEN, "Forbidden: insufficient permissions", request, null);
    }

    // 500 - Fallback for Unexpected Errors (NEVER expose stack traces!)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, HttpServletRequest request) {

        log.error("Unexpected error at {}:", request.getRequestURI(), ex);

        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please try again later.", request, null);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request){

        return buildResponse(HttpStatus.UNAUTHORIZED,"Invalid username or password", request, null);
    }

    // Helper method to avoid code duplication
    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message, HttpServletRequest request, Map<String, String> errors){
        ErrorResponse errorResponse = new ErrorResponse(status.value(), message, LocalDateTime.now(), request.getRequestURI(), errors);
        return new ResponseEntity<>(errorResponse,status);
    }
}
