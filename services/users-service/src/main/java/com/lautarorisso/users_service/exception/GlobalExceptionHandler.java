package com.lautarorisso.users_service.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
    String message = ex.getBindingResult().getFieldErrors().stream()
        .map(FieldError::getDefaultMessage)
        .collect(Collectors.joining("; "));
    return new ApiError(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(),
        "Bad Request", message, request.getRequestURI());
  }

  @ExceptionHandler(ValidationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError handleValidationException(ValidationException ex, HttpServletRequest request) {
    return new ApiError(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(),
        "Bad Request", ex.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
    return new ApiError(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(),
        "Bad Request", "Malformed JSON request body", request.getRequestURI());
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ApiError handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
    return new ApiError(LocalDateTime.now(), HttpStatus.NOT_FOUND.value(),
        "Not Found", ex.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(NoResourceFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ApiError handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
    return new ApiError(LocalDateTime.now(), HttpStatus.NOT_FOUND.value(),
        "Not Found", "Resource not found", request.getRequestURI());
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
  public ApiError handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
    return new ApiError(LocalDateTime.now(), HttpStatus.METHOD_NOT_ALLOWED.value(),
        "Method Not Allowed", "HTTP method not supported for this endpoint", request.getRequestURI());
  }

  @ExceptionHandler(ServiceUnavailableException.class)
  @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
  public ApiError handleServiceUnavailable(ServiceUnavailableException ex, HttpServletRequest request) {
    log.error("Downstream service unavailable on request {}", request.getRequestURI(), ex);
    return new ApiError(LocalDateTime.now(), HttpStatus.SERVICE_UNAVAILABLE.value(),
        "Service Unavailable", ex.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ApiError handleUnexpected(Exception ex, HttpServletRequest request) {
    log.error("Unexpected error on request {}", request.getRequestURI(), ex);
    return new ApiError(LocalDateTime.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(),
        "Internal Server Error", "An unexpected error occurred", request.getRequestURI());
  }
}