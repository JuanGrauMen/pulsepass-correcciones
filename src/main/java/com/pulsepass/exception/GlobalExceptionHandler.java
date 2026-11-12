package com.pulsepass.exception;

import com.pulsepass.dto.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex) {

        Map<String, String> details = new LinkedHashMap<>();

        for (FieldError fieldError :
                ex.getBindingResult().getFieldErrors()) {
            details.putIfAbsent(
                    fieldError.getField(),
                    fieldError.getDefaultMessage() == null
                            ? "Invalid value"
                            : fieldError.getDefaultMessage()
            );
        }

        return build(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                details
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedRequest(
            HttpMessageNotReadableException ex) {

        return build(
                HttpStatus.BAD_REQUEST,
                "Malformed request",
                Map.of()
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex) {

        return build(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                Map.of()
        );
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(
            DuplicateResourceException ex) {

        return build(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                Map.of()
        );
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(
            BusinessRuleException ex) {

        return build(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                Map.of()
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationException ex) {

        return build(
                HttpStatus.UNAUTHORIZED,
                "Invalid credentials",
                Map.of()
        );
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ErrorResponse> handleInvalidRequestParameters(Exception ex) {
        return build(
                HttpStatus.BAD_REQUEST,
                "Invalid request parameters",
                Map.of("request", "A required parameter is missing or has an invalid format")
        );
    }

    /**
     * Un usuario autenticado sin el rol requerido (por ejemplo, rechazado por
     * un @PreAuthorize dentro del controlador) debe recibir 403, no 500.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex) {

        return build(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                Map.of()
        );
    }

    /**
     * Errores del propio framework con su codigo HTTP definido: ruta
     * inexistente (404), metodo no admitido (405), tipo de contenido no
     * admitido (415) y respuesta no negociable (406).
     */
    @ExceptionHandler({
            NoResourceFoundException.class,
            HttpRequestMethodNotSupportedException.class,
            HttpMediaTypeNotSupportedException.class,
            HttpMediaTypeNotAcceptableException.class
    })
    public ResponseEntity<ErrorResponse> handleFrameworkHttpErrors(Exception ex) {
        HttpStatus status = HttpStatus.valueOf(
                ((org.springframework.web.ErrorResponse) ex).getStatusCode().value());

        String message = switch (status) {
            case NOT_FOUND -> "Resource not found";
            case METHOD_NOT_ALLOWED -> "Method not allowed";
            case UNSUPPORTED_MEDIA_TYPE -> "Unsupported media type";
            default -> "Not acceptable";
        };

        return build(status, message, Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception ex) {

        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected internal error occurred",
                Map.of()
        );
    }

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status,
            String message,
            Map<String, String> details) {

        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                details
        );

        return ResponseEntity.status(status).body(body);
    }
}