package com.example.demo.endpoint.rest.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionResponse {
  private UUID id;
  private String email;
  private String thumbnailKey;
  private Instant createdAt;
}
