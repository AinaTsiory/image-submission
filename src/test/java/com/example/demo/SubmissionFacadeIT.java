package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.endpoint.event.model.ThumbnailRequested;
import com.example.demo.endpoint.rest.dto.SubmissionResponse;
import com.example.demo.file.bucket.BucketComponent;
import com.example.demo.mail.Email;
import com.example.demo.mail.Mailer;
import com.example.demo.repository.SubmissionRepository;
import com.example.demo.repository.model.Submission;
import com.example.demo.service.event.ThumbnailRequestedService;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SubmissionFacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private SubmissionRepository submissionRepository;
  @Autowired private ThumbnailRequestedService thumbnailRequestedService;

  @MockBean private BucketComponent bucketComponent;
  @MockBean private Mailer mailer;

  private Resource getTestImageResource() {
    File rootFile = new File("test-image.png");
    if (rootFile.exists()) {
      return new FileSystemResource(rootFile);
    }
    return new ClassPathResource("test-image.png");
  }

  @BeforeEach
  void cleanUp() {
    submissionRepository.deleteAll();
  }

  @Test
  void createSubmission_returns201WithNullThumbnailKey() {
    var body = new LinkedMultiValueMap<String, Object>();

    var fileResource = getTestImageResource();
    var fileHeader = new HttpHeaders();
    fileHeader.setContentType(MediaType.IMAGE_PNG);
    var fileEntity = new HttpEntity<>(fileResource, fileHeader);

    body.add("file", fileEntity);
    body.add("email", "toky@example.com");

    var headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    var request = new HttpEntity<>(body, headers);

    var response = restTemplate.postForEntity("/submissions", request, SubmissionResponse.class);

    assertThat(response.getStatusCode())
        .withFailMessage(
            "Requete echouee avec le statut: %s et corps: %s",
            response.getStatusCode(), response.getBody())
        .isEqualTo(HttpStatus.CREATED);

    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getThumbnailKey()).isNull();
    assertThat(response.getBody().getEmail()).isEqualTo("toky@example.com");
  }

  @Test
  void listSubmissions_returnsPersistedSubmissions() {
    var submission =
        Submission.builder()
            .id(UUID.randomUUID())
            .email("a@example.com")
            .thumbnailKey(null)
            .createdAt(Instant.now())
            .build();
    submissionRepository.save(submission);

    var response = restTemplate.getForEntity("/submissions", SubmissionResponse[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody())
        .extracting(SubmissionResponse::getId)
        .contains(submission.getId());
  }

  @Test
  void thumbnailWorker_resizesUpdatesAndSendsEmail() throws Exception {
    var id = UUID.randomUUID();
    submissionRepository.save(
        Submission.builder()
            .id(id)
            .email("b@example.com")
            .thumbnailKey(null)
            .createdAt(Instant.now())
            .build());

    var tempOriginal = File.createTempFile("original", ".png");
    Files.write(tempOriginal.toPath(), getTestImageResource().getInputStream().readAllBytes());
    when(bucketComponent.download(anyString())).thenReturn(tempOriginal);
    when(bucketComponent.presign(anyString(), any(Duration.class)))
        .thenReturn(new URL("https://example.com/thumb.png"));

    thumbnailRequestedService.accept(
        ThumbnailRequested.builder()
            .submissionId(id)
            .originalKey("originals/" + id + ".png")
            .email("b@example.com")
            .build());

    var updated = submissionRepository.findById(id).orElseThrow();
    assertThat(updated.getThumbnailKey()).isNotNull();
    verify(mailer).accept(any(Email.class));
  }
}
