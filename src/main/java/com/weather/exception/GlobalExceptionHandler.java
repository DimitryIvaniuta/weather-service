package com.weather.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Global exception handler to translate exceptions into
 * consistent JSON problem responses.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle beans validation errors on @RequestBody bodies.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail onMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Validation failed");
        pd.setTitle("Invalid request");
        pd.setProperty("timestamp", LocalDateTime.now());
        pd.setProperty("path", request.getRequestURI());

        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> Optional.ofNullable(fe.getDefaultMessage())
                                .orElse(""),
                        // if two errors on the same field, keep the first message
                        (msg1, msg2) -> msg1
                ));

        pd.setProperty("errors", errors);
        return pd;
    }


    /**
     * Handle validation errors on @RequestParam, @PathVariable, etc.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail onConstraintViolation(ConstraintViolationException ex,
                                               HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, "Parameter validation failed");
        pd.setTitle("Invalid parameters");
        pd.setProperty("timestamp", LocalDateTime.now());
        pd.setProperty("path", request.getRequestURI());
        // collect path→message for each violation
        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        cv -> cv.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (m1, m2) -> m1
                ));
        pd.setProperty("errors", errors);
        return pd;
    }

    /**
     * Handle ResponseStatusException thrown in controllers or services.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail onResponseStatus(ResponseStatusException ex,
                                          HttpServletRequest request) {
        HttpStatusCode status = ex.getStatusCode();
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, ex.getReason());
        pd.setTitle(status.toString());
        pd.setProperty("timestamp", LocalDateTime.now());
        pd.setProperty("path", request.getRequestURI());
        log.error("ResponseStatus Error: ", ex);
        return pd;
    }

    /**
     * Catch-all for any other exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail onAllExceptions(Exception ex,
                                         HttpServletRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, "An unexpected error occurred");
        pd.setTitle("Internal Server Error");
        pd.setProperty("timestamp", LocalDateTime.now());
        pd.setProperty("path", request.getRequestURI());
        log.error("Global Error: ", ex);
        return pd;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail onMissingParam(MissingServletRequestParameterException ex,
                                        HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Required parameter '" + ex.getParameterName() + "' is missing"
        );
        pd.setProperty("path", req.getRequestURI());
        return pd;
    }
}
