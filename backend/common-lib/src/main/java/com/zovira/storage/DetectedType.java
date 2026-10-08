package com.zovira.storage;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

/**
 * File types identified from their leading bytes. The client-supplied filename and Content-Type
 * are never trusted: a script renamed to {@code photo.jpg} is rejected here.
 */
public enum DetectedType {
    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp"),
    AVIF("image/avif", "avif"),
    GLB("model/gltf-binary", "glb"),
    USDZ("model/vnd.usdz+zip", "usdz"),
    PDF("application/pdf", "pdf");

    private final String contentType;
    private final String extension;

    DetectedType(String contentType, String extension) {
        this.contentType = contentType;
        this.extension = extension;
    }

    public String contentType() {
        return contentType;
    }

    public String extension() {
        return extension;
    }

    public static Optional<DetectedType> detect(byte[] head) {
        if (startsWith(head, 0xFF, 0xD8, 0xFF)) {
            return Optional.of(JPEG);
        }
        if (startsWith(head, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return Optional.of(PNG);
        }
        if (ascii(head, 0, "RIFF") && ascii(head, 8, "WEBP")) {
            return Optional.of(WEBP);
        }
        if (ascii(head, 4, "ftypavif") || ascii(head, 4, "ftypavis")) {
            return Optional.of(AVIF);
        }
        if (ascii(head, 0, "glTF")) {
            return Optional.of(GLB);
        }
        if (startsWith(head, 0x50, 0x4B, 0x03, 0x04)) {
            return Optional.of(USDZ);
        }
        if (ascii(head, 0, "%PDF-")) {
            return Optional.of(PDF);
        }
        return Optional.empty();
    }

    private static boolean startsWith(byte[] data, int... signature) {
        if (data.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((data[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean ascii(byte[] data, int offset, String text) {
        byte[] expected = text.getBytes(StandardCharsets.US_ASCII);
        return data.length >= offset + expected.length
                && Arrays.equals(data, offset, offset + expected.length, expected, 0, expected.length);
    }
}
