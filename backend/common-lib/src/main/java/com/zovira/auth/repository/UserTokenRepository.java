package com.zovira.auth.repository;

import com.zovira.auth.entity.UserToken;
import com.zovira.auth.entity.UserTokenType;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserTokenRepository extends MongoRepository<UserToken, Long>, UserTokenRepositoryCustom {

    Optional<UserToken> findByTokenHashAndType(String tokenHash, UserTokenType type);

    default Optional<UserToken> findByHashAndType(String hash, UserTokenType type) {
        return findByTokenHashAndType(hash, type);
    }
}
