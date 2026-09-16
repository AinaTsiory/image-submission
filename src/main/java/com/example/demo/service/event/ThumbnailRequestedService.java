package com.example.demo.service.event;

import com.example.demo.endpoint.event.model.ThumbnailRequested;
import com.example.demo.file.bucket.BucketComponent;
import com.example.demo.mail.Email;
import com.example.demo.mail.Mailer;
import com.example.demo.repository.SubmissionRepository;
import jakarta.mail.internet.InternetAddress;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ThumbnailRequestedService implements Consumer<ThumbnailRequested> {

  private static final int THUMBNAIL_SIZE = 256;

  private final BucketComponent bucketComponent;
  private final SubmissionRepository submissionRepository;
  private final Mailer mailer;

  @SneakyThrows
  @Override
  public void accept(ThumbnailRequested event) {
    var originalFile = bucketComponent.download(event.getOriginalKey());
    var format = event.getOriginalKey().endsWith(".png") ? "png" : "jpg";

    var sourceImage = ImageIO.read(originalFile);
    var imageType = format.equals("png") ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
    var thumbnailImage = new BufferedImage(THUMBNAIL_SIZE, THUMBNAIL_SIZE, imageType);
    Graphics2D graphics = thumbnailImage.createGraphics();
    graphics.drawImage(sourceImage, 0, 0, THUMBNAIL_SIZE, THUMBNAIL_SIZE, null);
    graphics.dispose();

    var thumbnailFile = File.createTempFile("thumbnail-" + event.getSubmissionId(), "." + format);
    ImageIO.write(thumbnailImage, format, thumbnailFile);

    var thumbnailKey = "thumbnails/" + event.getSubmissionId() + "." + format;
    bucketComponent.upload(thumbnailFile, thumbnailKey);

    submissionRepository
        .findById(event.getSubmissionId())
        .ifPresent(
            submission -> {
              submission.setThumbnailKey(thumbnailKey);
              submissionRepository.save(submission);
            });

    var downloadLink = bucketComponent.presign(thumbnailKey, Duration.ofDays(7)).toString();

    mailer.accept(
        new Email(
            new InternetAddress(event.getEmail()),
            List.of(),
            List.of(),
            "Your thumbnail is ready",
            "Your thumbnail has been generated. Download it here: " + downloadLink,
            List.of()));
  }
}
