package com.careerpilot.exception;

import com.careerpilot.ai.AiClientException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        return errorResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(fieldError ->
                fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage())
        );

        return errorResponse(
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                request.getRequestURI(),
                fieldErrors
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedJson(HttpServletRequest request) {
        return errorResponse(
                HttpStatus.BAD_REQUEST,
                "Malformed JSON request",
                request.getRequestURI(),
                Map.of()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        return errorResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid value for " + exception.getName(),
                request.getRequestURI(),
                Map.of()
        );
    }

    @ExceptionHandler(AiClientException.class)
    public ResponseEntity<ApiErrorResponse> handleAiClientFailure(
            AiClientException exception,
            HttpServletRequest request
    ) {
        LOGGER.warn("AI analysis request failed: {}", exception.getMessage());

        return errorResponse(
                HttpStatus.BAD_GATEWAY,
                "AI analysis service is temporarily unavailable",
                request.getRequestURI(),
                Map.of()
        );
    }

    @ExceptionHandler(ResumeFileExtractionException.class)
    public ResponseEntity<ApiErrorResponse> handleResumeExtractionFailure(
            ResumeFileExtractionException exception,
            HttpServletRequest request
    ) {
        return errorResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }

    @ExceptionHandler(JobActivityCompletionException.class)
    public ResponseEntity<ApiErrorResponse> handleJobActivityCompletionFailure(
            JobActivityCompletionException exception,
            HttpServletRequest request
    ) {
        return errorResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }

    @ExceptionHandler(JobActivityRescheduleException.class)
    public ResponseEntity<ApiErrorResponse> handleJobActivityRescheduleFailure(
            JobActivityRescheduleException exception,
            HttpServletRequest request
    ) {
        return errorResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }

    @ExceptionHandler(JobActivityCalendarException.class)
    public ResponseEntity<ApiErrorResponse> handleJobActivityCalendarFailure(
            JobActivityCalendarException exception,
            HttpServletRequest request
    ) {
        return errorResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }

    @ExceptionHandler(JobCsvImportException.class)
    public ResponseEntity<ApiErrorResponse> handleJobCsvImportFailure(
            JobCsvImportException exception,
            HttpServletRequest request
    ) {
        return errorResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleUploadTooLarge(HttpServletRequest request) {
        return errorResponse(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "Uploaded files must be 6 MB or smaller.",
                request.getRequestURI(),
                Map.of()
        );
    }

    private static ResponseEntity<ApiErrorResponse> errorResponse(
            HttpStatus status,
            String message,
            String path,
            Map<String, String> fieldErrors
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                fieldErrors
        );
        return ResponseEntity.status(status).body(response);
    }
}
