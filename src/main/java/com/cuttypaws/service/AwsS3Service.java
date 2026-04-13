
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
import java.util.List;
import java.util.UUID;

@Service
public class AwsS3Service {

    private static final List<String> ALLOWED_IMAGE_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private static final List<String> ALLOWED_VIDEO_TYPES = List.of(
            "video/mp4",
            "video/quicktime",
            "video/webm",
            "video/x-matroska"
    );

    private final String bucketName;
    private final String region;
    private final String cloudFrontBaseUrl;
    private final String fallbackBaseUrl;
    private final S3Client s3Client;

    public AwsS3Service(
            @Value("${aws.access-key-id}") String accessKey,
            @Value("${aws.secret-access-key}") String secretKey,
            @Value("${aws.s3.bucket-name}") String bucketName,
            @Value("${aws.region:us-east-1}") String region,
            @Value("${aws.cloudfront.base-url}") String cloudFrontBaseUrl,
            @Value("${media.fallback-base-url:}") String fallbackBaseUrl
    ) {
        this.bucketName = bucketName;
        this.region = region;
        this.cloudFrontBaseUrl = normalizeBaseUrl(cloudFrontBaseUrl);
        this.fallbackBaseUrl = normalizeOptionalBaseUrl(fallbackBaseUrl);

        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        this.s3Client = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of(region))
                .build();
    }

    public String uploadMedia(MultipartFile file) {
        return uploadMedia(file, "uploads");
    }

    public String uploadMedia(MultipartFile file, String folder) {
        validateMedia(file);

        String contentType = file.getContentType();
        String extension = extractExtension(file.getOriginalFilename(), contentType);

        String safeFolder = sanitizeFolder(folder);
        String key = safeFolder + "/" + UUID.randomUUID() + extension;

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .cacheControl(buildCacheControl(contentType))
                    .build();

            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );

            return buildDeliveryUrl(key);

        } catch (IOException e) {
            throw new RuntimeException("Upload failed: " + e.getMessage(), e);
        }
    }

    /**
     * Best practice:
     * store this key in the database when possible.
     */
    public String buildObjectKey(String folder, String filename) {
        String safeFolder = sanitizeFolder(folder);
        String safeFilename = filename == null ? UUID.randomUUID().toString() : filename.trim();
        return safeFolder + "/" + safeFilename;
    }

    public String buildDeliveryUrl(String key) {
        String normalizedKey = normalizeKey(key);
        return cloudFrontBaseUrl + "/" + normalizedKey;
    }

    /**
     * Optional emergency fallback.
     * Use only if you truly have a second public delivery layer or signed access.
     */
    public String buildFallbackUrl(String key) {
        String normalizedKey = normalizeKey(key);

        if (fallbackBaseUrl != null && !fallbackBaseUrl.isBlank()) {
            return fallbackBaseUrl + "/" + normalizedKey;
        }

        // This only works if S3 access is public or presigned.
        return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + normalizedKey;
    }

    public String extractKeyFromUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return null;
        }

        URI uri = URI.create(fileUrl);
        String path = uri.getPath();

        if (path == null || path.isBlank() || "/".equals(path)) {
            return null;
        }

        return normalizeKey(path);
    }

    public void deleteMedia(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        try {
            String key = extractKeyFromUrl(fileUrl);

            if (key == null || key.isBlank()) {
                return;
            }

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);

        } catch (Exception e) {
            throw new RuntimeException("Failed to delete media: " + e.getMessage(), e);
        }
    }

    private void validateMedia(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is required");
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new RuntimeException("File content type is missing");
        }

        boolean allowed = ALLOWED_IMAGE_TYPES.contains(contentType)
                || ALLOWED_VIDEO_TYPES.contains(contentType);

        if (!allowed) {
            throw new RuntimeException("Only supported image and video files are allowed");
        }
    }

    private String extractExtension(String originalFilename, String contentType) {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase();
        }

        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            case "video/mp4" -> ".mp4";
            case "video/quicktime" -> ".mov";
            case "video/webm" -> ".webm";
            case "video/x-matroska" -> ".mkv";
            default -> "";
        };
    }

    private String sanitizeFolder(String folder) {
        if (folder == null || folder.isBlank()) {
            return "uploads";
        }

        return folder.trim()
                .replace("\\", "/")
                .replaceAll("^/+", "")
                .replaceAll("/+$", "");
    }

    private String normalizeKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Media key cannot be blank");
        }

        return key.trim().replace("\\", "/").replaceAll("^/+", "");
    }

    private String normalizeBaseUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("aws.cloudfront.base-url is required");
        }

        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String normalizeOptionalBaseUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }

        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String buildCacheControl(String contentType) {
        if (contentType != null && contentType.startsWith("image/")) {
            return "public, max-age=31536000, immutable";
        }

        if (contentType != null && contentType.startsWith("video/")) {
            return "public, max-age=31536000, immutable";
        }

        return "public, max-age=86400";
    }
}