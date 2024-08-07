package com.tranhuy105.musicserviceapi.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.tranhuy105.musicserviceapi.model.MediaItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Date;

@Service
public class S3Service implements StorageService{
    private final AmazonS3 s3Client;
    @Value("${s3.bucket.name}")
    private String bucketName;

    @Value("${s3.access.key}")
    private String accessKey;

    @Value("${s3.secret.key}")
    private String secretKey;

    @Value("${s3.endpoint.url}")
    private String serviceEndpoint;

    @Value("${s3.region}")
    private String region;
    private static final long URL_EXPIRATION_TIME_MILLIS = 1000 * 60 * 60; // 1 hour
    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);


    public S3Service(@Value("${s3.access.key}") String accessKey,
                     @Value("${s3.secret.key}") String secretKey,
                     @Value("${s3.endpoint.url}") String serviceEndpoint,
                     @Value("${s3.region}") String region) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.serviceEndpoint = serviceEndpoint;
        this.region = region;

        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(this.accessKey, this.secretKey);
        this.s3Client = AmazonS3Client.builder()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(this.serviceEndpoint, this.region))
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .withPathStyleAccessEnabled(true)
                .build();
    }

    @Override
    public URL generatePresignedUrl(MediaItem mediaItem) {
        String uri = mediaItem.getURI();
        String itemId = extractIdFromURI(uri);
        String type = extractTypeFromURI(uri);

        if (itemId == null || type == null) {
            throw new IllegalArgumentException("Invalid URI format");
        }

        GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, type + "/" + itemId)
                .withMethod(HttpMethod.GET)
                .withExpiration(getExpiration());
        return s3Client.generatePresignedUrl(generatePresignedUrlRequest);
    }

    @Override
    public void uploadMediaItem(File file, String mediaId, String mediaType) {
        try {
            String s3Key = String.format("%s/%s", mediaType, mediaId);

            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, s3Key, file);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.length());
            putObjectRequest.setMetadata(metadata);

            s3Client.putObject(putObjectRequest);
            logger.info("Upload completed with mediaId: " + mediaId + " and mediaType: " + mediaType);
        } catch (Exception e) {
            logger.error("Error uploading to S3: " + e.getMessage(), e);
            throw new RuntimeException("Error uploading to S3", e);
        }
    }


    private String extractIdFromURI(String uri) {
        if (uri == null || uri.isEmpty()) {
            return null;
        }

        // Extract the ID based on URI format
        // Example format: spotify:track:12345 or spotify:ad:67890
        String[] parts = uri.split(":");
        if (parts.length == 3) {
            return parts[2];
        }

        return null;
    }

    private String extractTypeFromURI(String uri) {
        if (uri == null || uri.isEmpty()) {
            return null;
        }

        String[] parts = uri.split(":");
        if (parts.length == 3) {
            return parts[1];
        }

        return null;
    }

    private Date getExpiration() {
        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += URL_EXPIRATION_TIME_MILLIS;
        expiration.setTime(expTimeMillis);
        return expiration;
    }

    private boolean doesObjectExist(String trackId) {
        try {
            s3Client.getObjectMetadata(bucketName, trackId);
            return true;
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() == 404) {
                return false;
            } else {
                throw e;
            }
        }
    }
}
