package com.zovira.audit.service;

import com.zovira.audit.entity.AuditLog;
import com.zovira.audit.repository.AuditLogRepository;
import com.zovira.security.AuthUser;
import com.zovira.security.CurrentUserArgumentResolver;
import java.time.Clock;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Records security-relevant and administrative actions. Entries are written in their own
 * transaction so they persist even when the surrounding operation fails.
 */
@Service
public class AuditService {

    private final AuditLogRepository repository;
    private final Clock clock;

    public AuditService(AuditLogRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String entityType, Object entityId, Map<String, Object> details) {
        AuthUser actor = CurrentUserArgumentResolver.current();
        record(actor == null ? null : actor.id(), actor == null ? null : actor.email(), action, entityType, entityId,
                details);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long actorId, String actorEmail, String action, String entityType, Object entityId,
            Map<String, Object> details) {
        repository.save(new AuditLog(actorId, actorEmail, action, entityType,
                entityId == null ? null : String.valueOf(entityId), details, currentIp(), clock.instant()));
    }

    private static String currentIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest().getRemoteAddr();
        }
        return null;
    }
}
