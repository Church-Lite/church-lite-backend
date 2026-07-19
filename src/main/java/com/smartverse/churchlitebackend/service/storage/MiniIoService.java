package com.smartverse.churchlitebackend.service.storage;


import com.potatotech.authorization.exception.ServiceException;
import com.potatotech.authorization.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.net.URI;
import java.net.URL;
import java.time.Duration;

@Service
public class MiniIoService {

    private static final String STORAGE_HOST = System.getenv("STORAGE_HOST");
    private static final Region REGION = Region.US_EAST_1;
    private final S3Presigner presigner;
    private final S3Client s3Client;

    public MiniIoService() {

        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create("admin", "admin123");



        this.presigner = S3Presigner.builder()
                .endpointOverride(URI.create(STORAGE_HOST))
                .region(REGION)
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();

        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(STORAGE_HOST))
                .region(REGION)
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .forcePathStyle(true) // MUITO IMPORTANTE para Wasabi
                .build();
    }

    public URL requestUpload(String objectKey, int expirationInSeconds) {
        createBucket();

        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(
                builder -> builder.signatureDuration(Duration.ofSeconds(expirationInSeconds))
                        .putObjectRequest(por -> por
                                .bucket(getTenantBucketName())
                                .contentType("application/octet-stream")
                                .key(objectKey))
        );
        return presignedRequest.url();
    }

    public URL requestDownload(String objectKey, int expirationInSeconds) {
        System.out.println(STORAGE_HOST);
        PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(
                builder -> builder.signatureDuration(Duration.ofSeconds(expirationInSeconds))
                        .getObjectRequest(gor -> gor
                                .bucket(getTenantBucketName())
                                .key(objectKey))
        );
        return presignedRequest.url();
    }

    public boolean requestDelete(String objectKey) {
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(getTenantBucketName())
                .key(objectKey)
                .build();

        var output = s3Client.deleteObject(deleteRequest);
        return output.sdkHttpResponse().isSuccessful();
    }

    public void createBucket() {
        if (verifyExistingBucket()) return;

        try {
            CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                    .bucket(getTenantBucketName())
                    .build();

            s3Client.createBucket(createBucketRequest);

        } catch (S3Exception e) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    public boolean verifyExistingBucket() {
        try {
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(getTenantBucketName())
                    .build();
            s3Client.headBucket(headBucketRequest);
            return true;
        } catch (S3Exception e) {
            if ("NoSuchBucket".equals(e.awsErrorDetails().errorCode())) {
                return false;
            } else {
                throw new ServiceException(HttpStatus.BAD_REQUEST, e.getMessage());
            }
        }
    }

    public void close() {
        presigner.close();
        s3Client.close();
    }

    private String getTenantBucketName() {
        return String.format("smart-church-%s", TenantContext.getCurrentTenant().replace("_", "-").toLowerCase());
    }
}