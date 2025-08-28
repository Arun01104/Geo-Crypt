package com.Geo_crypt.Backend.Service;


import io.minio.*;

import javax.annotation.PostConstruct;

import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Service
public class MinioService {


    private final MinioClient minioClient;


    @Value("${minio.bucket-encrypted}")
    private String encryptedBucket;


    @Value("${minio.bucket-decrypted}")
    private String decryptedBucket;


    public MinioService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }


    @PostConstruct
    public void init() throws Exception {
        ensureBucket(encryptedBucket);
        ensureBucket(decryptedBucket);
    }


    private void ensureBucket(String bucketName) throws Exception {
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }


    public void upload(String bucketName, String objectName, InputStream stream, long size, String contentType) throws Exception {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(stream, size, -1)
                        .contentType(contentType == null ? "application/octet-stream" : contentType)
                        .build()
        );
    }


    public String presignedUrl(String bucketName, String objectName, int expirySeconds) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketName)
                        .object(objectName)
                        .expiry(expirySeconds, TimeUnit.SECONDS)
                        .build()
        );
    }


    public InputStream getObject(String bucketName, String objectName) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder().bucket(bucketName).object(objectName).build()
        );
    }
}
