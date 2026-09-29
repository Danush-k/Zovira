package com.zovira.storage.controller;

import com.zovira.security.AuthUser;
import com.zovira.security.CurrentUser;
import com.zovira.storage.FileKind;
import com.zovira.storage.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Media uploads. File types are verified from their leading bytes, not the filename, and the
 * storage key is generated server-side so callers cannot choose a path.
 */
@RestController
@Tag(name = "Uploads", description = "Product, review and profile media")
public class UploadController {

    private final StorageService storage;

    public UploadController(StorageService storage) {
        this.storage = storage;
    }

    @PostMapping(value = "/api/v1/seller/products/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Upload product images (JPEG, PNG, WebP or AVIF, up to 5 MB each)")
    public List<Map<String, Object>> productImages(@RequestParam("files") List<MultipartFile> files) {
        return files.stream().limit(12)
                .map(file -> storage.store(file, "products", FileKind.IMAGE))
                .map(stored -> Map.<String, Object>of("url", stored.url(), "size", stored.size()))
                .toList();
    }

    @PostMapping(value = "/api/v1/uploads/review-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload photos for a product review")
    public List<Map<String, Object>> reviewImages(@CurrentUser AuthUser user,
            @RequestParam("files") List<MultipartFile> files) {
        return files.stream().limit(5)
                .map(file -> storage.store(file, "reviews", FileKind.IMAGE))
                .map(stored -> Map.<String, Object>of("url", stored.url(), "size", stored.size()))
                .toList();
    }
}
