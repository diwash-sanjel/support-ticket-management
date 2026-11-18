package com.tothenew.tickets.web;

import com.tothenew.tickets.exception.InvalidRequestException;
import com.tothenew.tickets.exception.InvalidStatusTransitionException;
import com.tothenew.tickets.exception.TicketNotEditableException;
import com.tothenew.tickets.exception.TicketNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import java.time.Instant;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(TicketNotFoundException.class)
    ResponseEntity<ApiError> notFound(TicketNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "TICKET_NOT_FOUND", ex.getMessage(), req, null);
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    ResponseEntity<ApiError> invalidTransition(InvalidStatusTransitionException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "INVALID_STATUS_TRANSITION", ex.getMessage(), req, null);
    }

    @ExceptionHandler(TicketNotEditableException.class)
    ResponseEntity<ApiError> notEditable(TicketNotEditableException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "TICKET_NOT_EDITABLE", ex.getMessage(), req, null);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ApiError> concurrent(OptimisticLockingFailureException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "The ticket was modified by someone else. Reload and try again.", req, null);
    }

    @ExceptionHandler(InvalidRequestException.class)
    ResponseEntity<ApiError> invalidRequest(InvalidRequestException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage(), req, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalidBody(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ApiError.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> new ApiError.FieldError(f.getField(), f.getDefaultMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed", req, errors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ApiError> invalidParams(HandlerMethodValidationException ex, HttpServletRequest req) {
        List<ApiError.FieldError> errors = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> new ApiError.FieldError(
                                r.getMethodParameter().getParameterName(), e.getDefaultMessage())))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed", req, errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        // Name the offending field where we can, but never echo parser internals back to the client.
        Throwable cause = ex.getCause();
        if (cause instanceof UnrecognizedPropertyException unknown) {
            return validation(req, "Validation failed",
                    List.of(new ApiError.FieldError(unknown.getPropertyName(), "Unknown field")));
        }
        if (cause instanceof InvalidFormatException invalid && invalid.getTargetType() != null
                && invalid.getTargetType().isEnum() && !invalid.getPath().isEmpty()) {
            String field = invalid.getPath().get(invalid.getPath().size() - 1).getPropertyName();
            return validation(req, "Validation failed", List.of(new ApiError.FieldError(field,
                    "Invalid value. Allowed: " + allowedValues(invalid.getTargetType()))));
        }
        return validation(req, "Malformed JSON request body", null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> typeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        String message = "Invalid value '" + truncate(String.valueOf(ex.getValue())) + "'";
        if (ex.getRequiredType() != null && ex.getRequiredType().isEnum()) {
            message += ". Allowed: " + allowedValues(ex.getRequiredType());
        }
        return validation(req, "Validation failed", List.of(new ApiError.FieldError(ex.getName(), message)));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ApiError> missingParam(MissingServletRequestParameterException ex, HttpServletRequest req) {
        return validation(req, "Validation failed",
                List.of(new ApiError.FieldError(ex.getParameterName(), "is required")));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiError> unsupportedMediaType(HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
                "Content type not supported. Use application/json", req, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> noResource(NoResourceFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found", req, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> methodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Method not allowed", req, null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception ex, HttpServletRequest req) {
        log.error("Unexpected error on {}", req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected server error", req, null);
    }

    private ResponseEntity<ApiError> validation(HttpServletRequest req, String message,
                                                List<ApiError.FieldError> fieldErrors) {
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, req, fieldErrors);
    }

    private static String allowedValues(Class<?> enumType) {
        return Arrays.stream(enumType.getEnumConstants()).map(c -> ((Enum<?>) c).name())
                .collect(Collectors.joining(", "));
    }

    private static String truncate(String value) {
        return value.length() <= 40 ? value : value.substring(0, 40) + "...";
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String message,
                                           HttpServletRequest req, List<ApiError.FieldError> fieldErrors) {
        ApiError body = new ApiError(Instant.now(), status.value(), code, message, req.getRequestURI(), fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}
