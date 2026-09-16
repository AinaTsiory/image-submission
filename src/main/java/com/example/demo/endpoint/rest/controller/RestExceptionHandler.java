package com.example.demo.endpoint.rest.controller;

import com.example.demo.endpoint.rest.dto.ErrorResponse;
import com.example.demo.service.InvalidSubmissionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {

  @ExceptionHandler(InvalidSubmissionException.class)
  public ResponseEntity<ErrorResponse> handleInvalidSubmission(
      InvalidSubmissionException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse(exception.getMessage()));
  }
}
