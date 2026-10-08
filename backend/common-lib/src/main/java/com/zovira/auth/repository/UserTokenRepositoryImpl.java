package com.zovira.auth.repository;

import com.zovira.auth.entity.UserToken;
import com.zovira.auth.entity.UserTokenType;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import java.time.Instant;

@Repository
public class UserTokenRepositoryImpl implements UserTokenRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public UserTokenRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public int invalidateOutstanding(Long userId, UserTokenType type, Instant now) {
        Query query = new Query(Criteria.where("userId").is(userId)
                .and("type").is(type)
                .and("usedAt").is(null));
        Update update = new Update().set("usedAt", now);
        UpdateResult result = mongoTemplate.updateMulti(query, update, UserToken.class);
        return (int) result.getModifiedCount();
    }

    @Override
    public int deleteExpiredBefore(Instant cutoff) {
        Query query = new Query(Criteria.where("expiresAt").lt(cutoff));
        DeleteResult result = mongoTemplate.remove(query, UserToken.class);
        return (int) result.getDeletedCount();
    }
}
