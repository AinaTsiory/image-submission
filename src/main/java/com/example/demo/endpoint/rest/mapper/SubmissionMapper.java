package com.example.demo.endpoint.rest.mapper;

import com.example.demo.endpoint.rest.dto.SubmissionResponse;
import com.example.demo.repository.model.Submission;
import org.springframework.stereotype.Component;

@Component
public class SubmissionMapper {

  public SubmissionResponse toResponse(Submission submission) {
    return SubmissionResponse.builder()
        .id(submission.getId())
        .email(submission.getEmail())
        .thumbnailKey(submission.getThumbnailKey())
        .createdAt(submission.getCreatedAt())
        .build();
  }
}
