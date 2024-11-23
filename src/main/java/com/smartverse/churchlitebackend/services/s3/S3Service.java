package com.smartverse.churchlitebackend.services.s3;


import com.potatotech.authorization.exception.ServiceException;
import com.potatotech.authorization.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.net.URL;
import java.time.Duration;

@Service
public class S3Service {

    private static final Region REGION = Region.US_EAST_2;
    private final S3Presigner presigner;
    private final S3Client s3Client;

    public S3Service() {

        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(System.getenv("AWS_ACCESS"), System.getenv("AWS_SECRET"));

        this.presigner = S3Presigner.builder()
                .region(REGION)
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .build();

        this.s3Client = S3Client.builder()
                .region(REGION)
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .build();
    }

    public URL requestUpload(String objectKey, int expirationInSeconds) {

        createBucket();

        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(
                builder -> builder.signatureDuration(Duration.ofSeconds(expirationInSeconds))
                        .putObjectRequest(por -> por.bucket(TenantContext.getCurrentTenant().replace("_","-").toLowerCase()).key(objectKey))
        );
        return presignedRequest.url();
    }

    public URL requestDownload(String objectKey, int expirationInSeconds) {
        PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(
                builder -> builder.signatureDuration(Duration.ofSeconds(expirationInSeconds))
                        .getObjectRequest(gor -> gor.bucket(TenantContext.getCurrentTenant().replace("_","-").toLowerCase()).key(objectKey))
        );
        return presignedRequest.url();
    }

    public boolean requestDelete(String objectKey) {

        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(TenantContext.getCurrentTenant().replace("_","-").toLowerCase())
                .key(objectKey)
                .build();

        var output = s3Client.deleteObject(deleteRequest);
        return output.sdkHttpResponse().isSuccessful();
    }

    public void createBucket() {

        if(verifyExistingBucket()) return;

        try {
            CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                    .bucket(TenantContext.getCurrentTenant().replace("_","-").toLowerCase())
                    .build();

            CreateBucketResponse response = s3Client.createBucket(createBucketRequest);

        } catch (S3Exception e) {
            throw new ServiceException(HttpStatus.BAD_REQUEST,e.getMessage());
        }
    }

    public boolean verifyExistingBucket() {
        try {
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(TenantContext.getCurrentTenant().replace("_","-").toLowerCase())
                    .build();
            s3Client.headBucket(headBucketRequest);
            return true;
        } catch (S3Exception e) {
            if (e.awsErrorDetails().errorCode().equals("NoSuchBucket")) {
                return false;
            } else {
                throw new ServiceException(HttpStatus.BAD_REQUEST,e.getMessage());
            }
        }
    }

    public void close() {
        presigner.close();
        s3Client.close();
    }
}
