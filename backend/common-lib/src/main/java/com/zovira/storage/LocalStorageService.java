package com.zovira.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Stores objects on the local filesystem; served read-only under the configured public base URL. */
public class LocalStorageService extends StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    private final Path root;
    private final String publicBaseUrl;

    public LocalStorageService(Path root, String publicBaseUrl) {
        this.root = root.toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
        try {
            Files.createDirectories(this.root);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create storage root " + this.root, e);
        }
        log.info("Local media storage at {}", this.root);
    }

    public Path root() {
        return root;
    }

    @Override
    protected String put(String key, byte[] bytes, String contentType) {
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store " + key, e);
        }
        return publicBaseUrl + "/" + key;
    }

    @Override
    public void delete(String url) {
        if (url == null || !url.startsWith(publicBaseUrl + "/")) {
            return;
        }
        Path target = root.resolve(url.substring(publicBaseUrl.length() + 1)).normalize();
        if (!target.startsWith(root)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("Could not delete {}: {}", target, e.getMessage());
        }
    }
}
