package com.zovira.settings.service;

import com.zovira.common.exception.BusinessException;
import com.zovira.common.exception.ErrorCodes;
import com.zovira.common.exception.NotFoundException;
import com.zovira.config.CacheConfig;
import com.zovira.settings.dto.PlatformSettings;
import com.zovira.settings.dto.SettingResponse;
import com.zovira.settings.entity.PlatformSetting;
import com.zovira.settings.repository.PlatformSettingRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingsService {

    private final PlatformSettingRepository repository;
    private final Clock clock;

    public SettingsService(PlatformSettingRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Cacheable(cacheNames = CacheConfig.SETTINGS, key = "'all'")
    @Transactional(readOnly = true)
    public PlatformSettings current() {
        Map<String, String> v = repository.findAll().stream()
                .collect(Collectors.toMap(PlatformSetting::getKey, PlatformSetting::getValue));
        return new PlatformSettings(
                money(v, "shipping.free-threshold", "499"),
                money(v, "shipping.standard-fee", "40"),
                money(v, "shipping.express-fee", "99"),
                integer(v, "shipping.standard-days", 4),
                integer(v, "shipping.express-days", 1),
                bool(v, "payments.cod-enabled", true),
                money(v, "payments.cod-fee", "0"),
                money(v, "payments.cod-max-order", "50000"),
                integer(v, "orders.max-quantity-per-item", 10),
                bool(v, "reviews.auto-publish", true));
    }

    @Transactional(readOnly = true)
    public List<SettingResponse> list() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(PlatformSetting::getKey))
                .map(s -> new SettingResponse(s.getKey(), s.getValue(), s.getDescription(), s.getUpdatedAt()))
                .toList();
    }

    /** Validates each value against the setting's type before saving; unknown keys are rejected. */
    @CacheEvict(cacheNames = CacheConfig.SETTINGS, allEntries = true)
    @Transactional
    public List<SettingResponse> update(Map<String, String> values, Long actorId) {
        Map<String, PlatformSetting> existing = repository.findAll().stream()
                .collect(Collectors.toMap(PlatformSetting::getKey, Function.identity()));
        values.forEach((key, value) -> {
            PlatformSetting setting = existing.get(key);
            if (setting == null) {
                throw NotFoundException.of("Setting '" + key + "'");
            }
            String normalized = value == null ? "" : value.trim();
            validate(key, normalized);
            setting.update(normalized, actorId, clock.instant());
        });
        return list();
    }

    private static void validate(String key, String value) {
        try {
            if (key.endsWith("enabled") || key.endsWith("auto-publish")) {
                if (!value.equals("true") && !value.equals("false")) {
                    throw new IllegalArgumentException();
                }
            } else if (key.endsWith("days") || key.endsWith("per-item")) {
                int n = Integer.parseInt(value);
                if (n < 0 || n > 365) {
                    throw new IllegalArgumentException();
                }
            } else if (new BigDecimal(value).signum() < 0) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCodes.VALIDATION_FAILED, "Invalid value for " + key + ": " + value);
        }
    }

    private static BigDecimal money(Map<String, String> v, String key, String fallback) {
        return new BigDecimal(v.getOrDefault(key, fallback));
    }

    private static int integer(Map<String, String> v, String key, int fallback) {
        return v.containsKey(key) ? Integer.parseInt(v.get(key)) : fallback;
    }

    private static boolean bool(Map<String, String> v, String key, boolean fallback) {
        return v.containsKey(key) ? Boolean.parseBoolean(v.get(key)) : fallback;
    }
}
