package com.zovira.storage;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

@Configuration
public class StorageConfig implements WebMvcConfigurer {

    private final StorageProperties properties;

    public StorageConfig(StorageProperties properties) {
        this.properties = properties;
    }

    @Bean
    StorageService storageService() {
        if ("s3".equalsIgnoreCase(properties.provider())) {
            StorageProperties.S3 s3 = properties.s3();
            S3ClientBuilder builder = S3Client.builder()
                    .region(Region.of(s3.region()))
                    .forcePathStyle(s3.pathStyleAccess());
            if (s3.endpoint() != null && !s3.endpoint().isBlank()) {
                builder.endpointOverride(URI.create(s3.endpoint()));
            }
            builder.credentialsProvider(s3.accessKey() != null && !s3.accessKey().isBlank()
                    ? StaticCredentialsProvider.create(AwsBasicCredentials.create(s3.accessKey(), s3.secretKey()))
                    : DefaultCredentialsProvider.builder().build());
            String publicBase = s3.publicBaseUrl() != null && !s3.publicBaseUrl().isBlank()
                    ? s3.publicBaseUrl()
                    : (s3.endpoint() == null || s3.endpoint().isBlank()
                            ? "https://" + s3.bucket() + ".s3." + s3.region() + ".amazonaws.com"
                            : s3.endpoint().replaceAll("/+$", "") + "/" + s3.bucket());
            S3StorageService service = new S3StorageService(builder.build(), s3.bucket(), publicBase);
            service.ensureBucket();
            return service;
        }
        return new LocalStorageService(Path.of(properties.local().root()), properties.local().publicBaseUrl());
    }

    /** Serves locally stored media with long-lived caching; keys are random so content never changes. */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (!"s3".equalsIgnoreCase(properties.provider())) {
            String base = properties.local().publicBaseUrl().replaceAll("/+$", "");
            String location = Path.of(properties.local().root()).toAbsolutePath().normalize().toUri().toString();
            registry.addResourceHandler(base + "/**")
                    .addResourceLocations(location.endsWith("/") ? location : location + "/")
                    .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable());
        }
    }
}
