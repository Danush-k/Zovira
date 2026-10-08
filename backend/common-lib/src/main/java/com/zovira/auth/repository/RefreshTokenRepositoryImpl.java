package com.zovira.auth.repository;

import com.zovira.auth.entity.RefreshToken;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class RefreshTokenRepositoryImpl implements RefreshTokenRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public RefreshTokenRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<RefreshToken> findActiveByUser(Long userId, Instant now) {
        Query query = new Query(Criteria.where("userId").is(userId)
                .and("revokedAt").is(null)
                .and("expiresAt").gt(now))
                .with(Sort.by(Sort.Direction.DESC, "lastUsedAt"));
        return mongoTemplate.find(query, RefreshToken.class);
    }

    @Override
    public int revokeFamily(UUID familyId, Instant now, String reason) {
        Query query = new Query(Criteria.where("familyId").is(familyId)
                .and("revokedAt").is(null));
        Update update = new Update().set("revokedAt", now).set("revokedReason", reason);
        UpdateResult result = mongoTemplate.updateMulti(query, update, RefreshToken.class);
        return (int) result.getModifiedCount();
    }

    @Override
    public int revokeAllForUser(Long userId, Instant now, String reason) {
        Query query = new Query(Criteria.where("userId").is(userId)
                .and("revokedAt").is(null));
        Update update = new Update().set("revokedAt", now).set("revokedReason", reason);
        UpdateResult result = mongoTemplate.updateMulti(query, update, RefreshToken.class);
        return (int) result.getModifiedCount();
    }

    @Override
    public int deleteExpiredBefore(Instant cutoff) {
        Query query = new Query(Criteria.where("expiresAt").lt(cutoff));
        DeleteResult result = mongoTemplate.remove(query, RefreshToken.class);
        return (int) result.getDeletedCount();
    }
}
