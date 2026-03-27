package com.cuttypaws.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

@Service
public class AwsS3Service {

    private final String bucketName;
    private final String region;
    private final S3Client s3Client;

    public AwsS3Service(
            @Value("${aws.access-key-id}") String accessKey,
            @Value("${aws.secret-access-key}") String secretKey,
            @Value("${aws.s3.bucket-name}") String bucketName,
            @Value("${aws.region:us-east-1}") String region
    ) {
        this.bucketName = bucketName;
        this.region = region;

        AwsBasicCredentials creds = AwsBasicCredentials.create(accessKey, secretKey);
        this.s3Client = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(creds))
                .region(Region.of(region))
                .build();
    }

    public String uploadMedia(MultipartFile file) {
        return uploadMedia(file, "posts");
    }

    public String uploadMedia(MultipartFile file, String folder) {
        String contentType = file.getContentType();
        if (contentType == null || !(contentType.startsWith("image/") || contentType.startsWith("video/"))) {
            throw new RuntimeException("Only image and video files are allowed");
        }

        String ext = "";
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.'));
        }

        String safeFolder = (folder == null || folder.isBlank()) ? "uploads" : folder.trim();
        String key = safeFolder + "/" + UUID.randomUUID() + ext;

        try {
            PutObjectRequest put = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(put, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return buildPublicUrl(key);

        } catch (IOException e) {
            throw new RuntimeException("Upload failed: " + e.getMessage(), e);
        }
    }

    public String buildPublicUrl(String key) {
        return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + key;
    }

    public void deleteMedia(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        try {
            URI uri = URI.create(fileUrl);
            String path = uri.getPath();
            if (path == null || path.isBlank() || path.equals("/")) {
                return;
            }

            String key = path.startsWith("/") ? path.substring(1) : path;

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);

        } catch (Exception e) {
            throw new RuntimeException("Failed to delete media: " + e.getMessage(), e);
        }
    }
}