package com.zovira.settings.repository;

import com.zovira.settings.entity.PlatformSetting;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PlatformSettingRepository extends MongoRepository<PlatformSetting, String> {
}
