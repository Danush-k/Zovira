package com.zovira.auth.repository;

import com.zovira.auth.entity.RefreshToken;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface RefreshTokenRepositoryCustom {

    List<RefreshToken> findActiveByUser(Long userId, Instant now);

    int revokeFamily(UUID familyId, Instant now, String reason);

    int revokeAllForUser(Long userId, Instant now, String reason);

    int deleteExpiredBefore(Instant cutoff);
}
