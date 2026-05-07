package ru.pep.platform.storage;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
@ConditionalOnProperty(prefix = "pep.storage.s3", name = "enabled", havingValue = "true")
public class S3StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

    private final S3Client client;
    private final String bucket;
    private final String systemBucket;

    public S3StorageService(
            @Value("${pep.storage.s3.endpoint}") String endpoint,
            @Value("${pep.storage.s3.region}") String region,
            @Value("${pep.storage.s3.bucket}") String bucket,
            @Value("${pep.storage.s3.system-bucket:pep-system-tasks}") String systemBucket,
            @Value("${pep.storage.s3.access-key}") String accessKey,
            @Value("${pep.storage.s3.secret-key}") String secretKey,
            @Value("${pep.storage.s3.path-style-access:true}") boolean pathStyleAccess) {
        this.bucket = bucket;
        this.systemBucket = systemBucket;
        this.client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(pathStyleAccess)
                        .build())
                .httpClient(UrlConnectionHttpClient.builder().build())
                .build();
    }

    @PostConstruct
    void ensureBuckets() {
        for (String name : List.of(bucket, systemBucket)) {
            ensureBucket(name);
        }
    }

    private void ensureBucket(String name) {
        try {
            client.headBucket(HeadBucketRequest.builder().bucket(name).build());
            log.info("S3 bucket '{}' ready", name);
        } catch (NoSuchBucketException ignored) {
            createBucket(name);
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                createBucket(name);
            } else {
                log.warn("S3 head bucket '{}' failed: {}", name, exception.awsErrorDetails().errorMessage());
            }
        } catch (RuntimeException exception) {
            log.warn("S3 connectivity check failed for bucket '{}': {}", name, exception.getMessage());
        }
    }

    private void createBucket(String name) {
        try {
            client.createBucket(CreateBucketRequest.builder().bucket(name).build());
            log.info("S3 bucket '{}' created", name);
        } catch (RuntimeException exception) {
            log.warn("Failed to create S3 bucket '{}': {}", name, exception.getMessage());
        }
    }

    public String getBucket() {
        return bucket;
    }

    public String getSystemBucket() {
        return systemBucket;
    }

    public void uploadArchive(String key, Path archive, String contentType) {
        uploadToBucket(bucket, key, archive, contentType);
    }

    public void uploadSystemArchive(String key, Path archive, String contentType) {
        uploadToBucket(systemBucket, key, archive, contentType);
    }

    private void uploadToBucket(String targetBucket, String key, Path archive, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(targetBucket)
                .key(key)
                .contentType(contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType)
                .build();
        client.putObject(request, archive);
    }

    public void delete(String key) {
        deleteFromBucket(bucket, key);
    }

    public void deleteFromSystem(String key) {
        deleteFromBucket(systemBucket, key);
    }

    private void deleteFromBucket(String targetBucket, String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            client.deleteObject(DeleteObjectRequest.builder().bucket(targetBucket).key(key).build());
        } catch (RuntimeException exception) {
            log.warn("Failed to delete S3 object '{}/{}': {}", targetBucket, key, exception.getMessage());
        }
    }

    public void copyBetweenBuckets(String srcBucket, String srcKey, String dstBucket, String dstKey) {
        if (srcBucket == null || srcBucket.isBlank() || srcKey == null || srcKey.isBlank()) {
            throw new IllegalArgumentException("Source bucket/key required for S3 copy");
        }
        if (dstBucket == null || dstBucket.isBlank() || dstKey == null || dstKey.isBlank()) {
            throw new IllegalArgumentException("Destination bucket/key required for S3 copy");
        }
        CopyObjectRequest request = CopyObjectRequest.builder()
                .sourceBucket(srcBucket)
                .sourceKey(srcKey)
                .destinationBucket(dstBucket)
                .destinationKey(dstKey)
                .build();
        client.copyObject(request);
    }

    public void downloadFromSystem(String key, Path destination) {
        downloadObject(systemBucket, key, destination);
    }

    public void downloadFromUser(String key, Path destination) {
        downloadObject(bucket, key, destination);
    }

    public void downloadToFile(String sourceBucket, String key, Path destination) {
        downloadObject(sourceBucket, key, destination);
    }

    private void downloadObject(String targetBucket, String key, Path destination) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("S3 key required for download");
        }
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(targetBucket)
                .key(key)
                .build();
        try (ResponseInputStream<GetObjectResponse> response = client.getObject(request)) {
            Files.copy(response, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to download S3 object " + targetBucket + "/" + key, exception);
        }
    }

    @SuppressWarnings("unused")
    private void touch(Path path) throws IOException {
        Files.write(path, new byte[0], StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
