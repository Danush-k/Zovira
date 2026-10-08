package com.zovira.storage;

import com.zovira.common.exception.BusinessException;
import com.zovira.common.exception.ErrorCodes;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

/**
 * Object storage for user and seller uploaded media. Implementations only persist bytes; the
 * shared validation here enforces size limits and detected-type allow-lists, and generates the
 * object key so user input never influences the storage path.
 */
public abstract class StorageService {

    public record StoredFile(String key, String url, String contentType, long size) {
    }

    public StoredFile store(MultipartFile file, String folder, FileKind kind) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCodes.INVALID_FILE, "Choose a file to upload");
        }
        if (file.getSize() > kind.maxBytes()) {
            throw new BusinessException(ErrorCodes.INVALID_FILE,
                    "File is too large. The limit is " + (kind.maxBytes() / (1024 * 1024)) + " MB.");
        }
        try (InputStream in = file.getInputStream()) {
            return store(in.readAllBytes(), folder, kind);
        } catch (IOException e) {
            throw new BusinessException(ErrorCodes.INVALID_FILE, "The file could not be read");
        }
    }

    public StoredFile store(byte[] bytes, String folder, FileKind kind) {
        if (bytes.length == 0 || bytes.length > kind.maxBytes()) {
            throw new BusinessException(ErrorCodes.INVALID_FILE, "File is empty or too large");
        }
        DetectedType type = DetectedType.detect(Arrays.copyOf(bytes, Math.min(bytes.length, 32)))
                .filter(kind.allowed()::contains)
                .orElseThrow(() -> new BusinessException(ErrorCodes.INVALID_FILE,
                        "Unsupported file type. Allowed: " + kind.allowed()));
        LocalDate today = LocalDate.now();
        String key = "%s/%d/%02d/%s.%s".formatted(sanitizeFolder(folder), today.getYear(), today.getMonthValue(),
                UUID.randomUUID(), type.extension());
        String url = put(key, bytes, type.contentType());
        return new StoredFile(key, url, type.contentType(), bytes.length);
    }

    /** Persists the object and returns its public URL. */
    protected abstract String put(String key, byte[] bytes, String contentType);

    /** Deletes an object previously returned by {@link #store}; unknown URLs are ignored. */
    public abstract void delete(String url);

    private static String sanitizeFolder(String folder) {
        String cleaned = folder == null ? "misc" : folder.replaceAll("[^a-z0-9/-]", "").replaceAll("/{2,}", "/");
        cleaned = cleaned.replaceAll("^/+|/+$", "");
        return cleaned.isEmpty() ? "misc" : cleaned;
    }
}
