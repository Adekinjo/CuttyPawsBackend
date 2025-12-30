
package com.cuttypaws.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.IOException;

@Service
public class AwsS3Service {

    private final String bucketName = "beauthrist-ecommerce";

    @Value("${aws.s3.access}")
    private String awsS3AccessKey;

    @Value("${aws.s3.secrete}")
    private String awsS3SecretKey;

    public String saveImageToS3(MultipartFile photo) {
        try {
            String s3FileName = photo.getOriginalFilename();

            // Create AWS credentials using the access and secret key
            AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(awsS3AccessKey, awsS3SecretKey);

            // Create an S3 client with the credentials and region
            S3Client s3Client = S3Client.builder()
                    .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                    .region(Region.US_EAST_1)
                    .build();

            // Create a PutObjectRequest to upload the image to S3
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3FileName)
                    .contentType("image/jpeg") // Set the content type
                    .build();

            // Upload the file to S3
            PutObjectResponse response = s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(photo.getInputStream(), photo.getSize()));

            // Return the URL of the uploaded file
            return "https://" + bucketName + ".s3.us-east-1.amazonaws.com/" + s3FileName;

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to upload image to S3 bucket: " + e.getMessage());
        }
    }
}