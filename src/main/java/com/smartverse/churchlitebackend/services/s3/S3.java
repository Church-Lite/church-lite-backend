package com.smartverse.churchlitebackend.services.s3;

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Date;

@Service
public class S3 {

    private static final String REGION = "us-east-2"; // Região do bucket
    private static final String BUCKET_NAME = "smartchurch";

    private final AmazonS3 s3Client;

    public S3() {

        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(System.getenv("AWS_ACCESS"), System.getenv("AWS_SECRET"));
        this.s3Client = AmazonS3ClientBuilder.standard()
                .withRegion(REGION)
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .build();
    }

    public URL generatePresignedUrlForUpload(String objectKey, int expirationInSeconds) {
        return generatePresignedUrl(objectKey, expirationInSeconds, HttpMethod.PUT);
    }

    public URL generatePresignedUrlForDownload(String objectKey, int expirationInSeconds) {
        return generatePresignedUrl(objectKey, expirationInSeconds, HttpMethod.GET);
    }

    private URL generatePresignedUrl(String objectKey, int expirationInSeconds, HttpMethod method) {
        Date expiration = new Date(System.currentTimeMillis() + expirationInSeconds * 1000);
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(BUCKET_NAME, objectKey)
                .withMethod(method)
                .withExpiration(expiration);

        return s3Client.generatePresignedUrl(request);
    }
}
