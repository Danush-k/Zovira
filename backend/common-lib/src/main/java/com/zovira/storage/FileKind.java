package com.zovira.storage;

import java.util.Set;

/** Upload categories with their permitted detected types and size ceilings. */
public enum FileKind {
    IMAGE(Set.of(DetectedType.JPEG, DetectedType.PNG, DetectedType.WEBP, DetectedType.AVIF), 5L * 1024 * 1024),
    MODEL(Set.of(DetectedType.GLB), 25L * 1024 * 1024),
    AR_MODEL(Set.of(DetectedType.USDZ), 25L * 1024 * 1024),
    DOCUMENT(Set.of(DetectedType.PDF), 10L * 1024 * 1024);

    private final Set<DetectedType> allowed;
    private final long maxBytes;

    FileKind(Set<DetectedType> allowed, long maxBytes) {
        this.allowed = allowed;
        this.maxBytes = maxBytes;
    }

    public Set<DetectedType> allowed() {
        return allowed;
    }

    public long maxBytes() {
        return maxBytes;
    }
}
