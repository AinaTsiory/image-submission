package com.example.demo.endpoint.rest.controller;

import com.example.demo.endpoint.rest.dto.SubmissionResponse;
import com.example.demo.service.SubmissionService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@AllArgsConstructor
public class SubmissionController {

  private final SubmissionService submissionService;

  @PostMapping(value = "/submissions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<SubmissionResponse> createSubmission(
      @RequestParam("file") MultipartFile file, @RequestParam("email") String email) {
    var response = submissionService.createSubmission(file, email);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/submissions")
  public ResponseEntity<List<SubmissionResponse>> listSubmissions() {
    return ResponseEntity.ok(submissionService.listSubmissions());
  }
}
