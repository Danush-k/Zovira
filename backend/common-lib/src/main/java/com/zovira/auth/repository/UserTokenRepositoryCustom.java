package com.zovira.auth.repository;

import com.zovira.auth.entity.UserTokenType;
import java.time.Instant;

public interface UserTokenRepositoryCustom {

    int invalidateOutstanding(Long userId, UserTokenType type, Instant now);

    int deleteExpiredBefore(Instant cutoff);
}
