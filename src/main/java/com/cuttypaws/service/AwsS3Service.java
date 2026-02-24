//
package com.cuttypaws.service;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
//import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
//import software.amazon.awssdk.core.sync.RequestBody;
//import software.amazon.awssdk.regions.Region;
//import software.amazon.awssdk.services.s3.S3Client;
//import software.amazon.awssdk.services.s3.model.PutObjectRequest;
//import software.amazon.awssdk.services.s3.model.PutObjectResponse;
//
//import java.io.IOException;
//import java.util.UUID;

//@Service
//public class AwsS3Service {
//
//    private final String bucketName = "beauthrist-ecommerce";
//
//    @Value("${aws.s3.access}")
//    private String awsS3AccessKey;
//
//    @Value("${aws.s3.secrete}")
//    private String awsS3SecretKey;
//
//    public String saveImageToS3(MultipartFile photo) {
//        try {
//            String s3FileName = photo.getOriginalFilename();
//
//            // Create AWS credentials using the access and secret key
//            AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(awsS3AccessKey, awsS3SecretKey);
//
//            // Create an S3 client with the credentials and region
//            S3Client s3Client = S3Client.builder()
//                    .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
//                    .region(Region.US_EAST_1)
//                    .build();
//
//            // Create a PutObjectRequest to upload the image to S3
//            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
//                    .bucket(bucketName)
//                    .key(s3FileName)
//                    .contentType("image/jpeg") // Set the content type
//                    .build();
//
//            // Upload the file to S3
//            PutObjectResponse response = s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(photo.getInputStream(), photo.getSize()));
//
//            // Return the URL of the uploaded file
//            return "https://" + bucketName + ".s3.us-east-1.amazonaws.com/" + s3FileName;
//
//        } catch (IOException e) {
//            e.printStackTrace();
//            throw new RuntimeException("Unable to upload image to S3 bucket: " + e.getMessage());
//        }
//    }
//}


//@Service
//public class AwsS3Service {
//
//    private final String bucketName = "beauthrist-ecommerce";
//    private final S3Client s3Client;
//
//    public AwsS3Service(
//            @Value("${aws.s3.access}") String access,
//            @Value("${aws.s3.secrete}") String secret
//    ) {
//        AwsBasicCredentials creds = AwsBasicCredentials.create(access, secret);
//        this.s3Client = S3Client.builder()
//                .credentialsProvider(StaticCredentialsProvider.create(creds))
//                .region(Region.US_EAST_1)
//                .build();
//    }
//
//    public String uploadMedia(MultipartFile file) {
//        String contentType = file.getContentType();
//        if (contentType == null || !(contentType.startsWith("image/") || contentType.startsWith("video/"))) {
//            throw new RuntimeException("Only image and video files are allowed");
//        }
//
//        String ext = "";
//        String original = file.getOriginalFilename();
//        if (original != null && original.contains(".")) {
//            ext = original.substring(original.lastIndexOf('.'));
//        }
//
//        String key = "posts/" + UUID.randomUUID() + ext;
//
//        try {
//            PutObjectRequest put = PutObjectRequest.builder()
//                    .bucket(bucketName)
//                    .key(key)
//                    .contentType(contentType)
//                    .build();
//
//            s3Client.putObject(put, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
//
//            return "https://" + bucketName + ".s3.us-east-1.amazonaws.com/" + key;
//
//        } catch (IOException e) {
//            throw new RuntimeException("Upload failed: " + e.getMessage());
//        }
//    }
//}








import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
public class AwsS3Service {

    private final String bucketName;
    private final Region region;
    private final S3Client s3Client;

    public AwsS3Service(
            @Value("${aws.access-key-id}") String accessKey,
            @Value("${aws.secret-access-key}") String secretKey,
            @Value("${aws.region:us-east-1}") String regionStr,
            @Value("${aws.s3.bucket-name}") String bucketName
    ) {
        this.bucketName = bucketName;
        this.region = Region.of(regionStr);

        AwsBasicCredentials creds = AwsBasicCredentials.create(accessKey, secretKey);
        this.s3Client = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(creds))
                .region(this.region)
                .build();
    }

    public String uploadMedia(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !(contentType.startsWith("image/") || contentType.startsWith("video/"))) {
            throw new RuntimeException("Only image and video files are allowed");
        }

        String ext = "";
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.'));
        }

        String key = "posts/" + UUID.randomUUID() + ext;

        try {
            PutObjectRequest put = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(put, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            return "https://" + bucketName + ".s3." + region.id() + ".amazonaws.com/" + key;

        } catch (IOException e) {
            throw new RuntimeException("Upload failed: " + e.getMessage(), e);
        }
    }
}