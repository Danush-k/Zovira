package com.zovira.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * S3-compatible object storage (AWS S3, MinIO, Cloudflare R2, ...). Objects are immutable (keys
 * are random) so they are served with a one-year cache lifetime, ready to sit behind a CDN.
 */
public class S3StorageService extends StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

    private final S3Client client;
    private final String bucket;
    private final String publicBaseUrl;

    public S3StorageService(S3Client client, String bucket, String publicBaseUrl) {
        this.client = client;
        this.bucket = bucket;
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
    }

    /** Creates the bucket with anonymous read access when it does not exist (local MinIO setups). */
    public void ensureBucket() {
        try {
            client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException e) {
            createPublicBucket();
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                createPublicBucket();
            } else {
                log.warn("Could not verify bucket {}: {}", bucket, e.getMessage());
            }
        }
    }

    private void createPublicBucket() {
        client.createBucket(b -> b.bucket(bucket));
        String policy = """
                {"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"AWS":["*"]},\
                "Action":["s3:GetObject"],"Resource":["arn:aws:s3:::%s/*"]}]}""".formatted(bucket);
        client.putBucketPolicy(PutBucketPolicyRequest.builder().bucket(bucket).policy(policy).build());
        log.info("Created public-read bucket {}", bucket);
    }

    @Override
    protected String put(String key, byte[] bytes, String contentType) {
        client.putObject(PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .cacheControl("public, max-age=31536000, immutable")
                .build(), RequestBody.fromBytes(bytes));
        return publicBaseUrl + "/" + key;
    }

    @Override
    public void delete(String url) {
        if (url == null || !url.startsWith(publicBaseUrl + "/")) {
            return;
        }
        String key = url.substring(publicBaseUrl.length() + 1);
        try {
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (S3Exception e) {
            log.warn("Could not delete {}: {}", key, e.getMessage());
        }
    }
}
