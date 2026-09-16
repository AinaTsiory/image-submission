package com.example.demo.service;

import com.example.demo.endpoint.event.EventProducer;
import com.example.demo.endpoint.event.model.ThumbnailRequested;
import com.example.demo.endpoint.rest.dto.SubmissionResponse;
import com.example.demo.endpoint.rest.mapper.SubmissionMapper;
import com.example.demo.file.bucket.BucketComponent;
import com.example.demo.repository.SubmissionRepository;
import com.example.demo.repository.model.Submission;
import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@AllArgsConstructor
public class SubmissionService {

  private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/png", "image/jpeg");

  private final SubmissionRepository submissionRepository;
  private final BucketComponent bucketComponent;
  private final EventProducer<ThumbnailRequested> eventProducer;
  private final SubmissionMapper submissionMapper;

  @SneakyThrows
  public SubmissionResponse createSubmission(MultipartFile file, String email) {
    if (file == null || file.isEmpty()) {
      throw new InvalidSubmissionException("file must not be empty");
    }
    var contentType = file.getContentType();
    if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
      throw new InvalidSubmissionException("file must be a PNG or JPEG image");
    }

    var id = UUID.randomUUID();
    var extension = contentType.equals("image/png") ? "png" : "jpg";
    var originalKey = "originals/" + id + "." + extension;

    var tempFile = File.createTempFile("submission-" + id, "." + extension);
    file.transferTo(tempFile);
    bucketComponent.upload(tempFile, originalKey);

    var submission =
        Submission.builder()
            .id(id)
            .email(email)
            .thumbnailKey(null)
            .createdAt(Instant.now())
            .build();
    submissionRepository.save(submission);

    var event =
        ThumbnailRequested.builder().submissionId(id).originalKey(originalKey).email(email).build();
    eventProducer.accept(List.of(event));

    return submissionMapper.toResponse(submission);
  }

  public List<SubmissionResponse> listSubmissions() {
    return submissionRepository.findAll().stream().map(submissionMapper::toResponse).toList();
  }
}
