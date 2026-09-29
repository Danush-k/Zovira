package com.zovira.settings.controller;

import com.zovira.settings.dto.PlatformSettings;
import com.zovira.settings.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings")
@Tag(name = "Settings", description = "Public storefront configuration")
public class PublicSettingsController {

    private final SettingsService settingsService;

    public PublicSettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping("/public")
    @Operation(summary = "Shipping fees, delivery estimates and payment rules shown to shoppers")
    public PlatformSettings publicSettings() {
        return settingsService.current();
    }
}
