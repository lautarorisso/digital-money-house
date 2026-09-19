package com.lautarorisso.api_gateway;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

  @ExceptionHandler(ResponseStatusException.class)
  public ApiError handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
    int status = ex.getStatusCode().value();
    return new ApiError(LocalDateTime.now(), status,
        ex.getReason() != null ? ex.getReason() : "Error", ex.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ApiError handleUnexpected(Exception ex, HttpServletRequest request) {
    log.error("Unexpected error on request {}", request.getRequestURI(), ex);
    return new ApiError(LocalDateTime.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(),
        "Internal Server Error", "An unexpected error occurred", request.getRequestURI());
  }
}