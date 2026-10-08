package com.zovira.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "zovira.storage")
public record StorageProperties(
        @DefaultValue("local") String provider,
        @DefaultValue Local local,
        @DefaultValue S3 s3) {

    public record Local(@DefaultValue("./uploads") String root, @DefaultValue("/media") String publicBaseUrl) {
    }

    public record S3(String endpoint, @DefaultValue("ap-south-1") String region,
            @DefaultValue("zovira-media") String bucket, String accessKey, String secretKey, String publicBaseUrl,
            @DefaultValue("true") boolean pathStyleAccess) {
    }
}
