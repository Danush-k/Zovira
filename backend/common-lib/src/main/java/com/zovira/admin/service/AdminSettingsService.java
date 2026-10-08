package com.zovira.admin.service;

import com.zovira.audit.service.AuditService;
import com.zovira.settings.dto.SettingResponse;
import com.zovira.settings.service.SettingsService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminSettingsService {

    private final SettingsService settings;
    private final AuditService audit;

    public AdminSettingsService(SettingsService settings, AuditService audit) {
        this.settings = settings;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<SettingResponse> list() {
        return settings.list();
    }

    @Transactional
    public List<SettingResponse> update(Long adminId, Map<String, String> values) {
        List<SettingResponse> updated = settings.update(values, adminId);
        audit.record("SETTINGS_UPDATED", "SETTINGS", null, Map.copyOf(values));
        return updated;
    }
}
