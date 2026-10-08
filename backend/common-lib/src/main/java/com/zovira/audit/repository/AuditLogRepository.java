package com.zovira.audit.repository;

import com.zovira.audit.entity.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuditLogRepository extends MongoRepository<AuditLog, Long> {
}
