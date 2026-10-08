package com.zovira.user.repository;

import com.zovira.user.entity.User;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    default Optional<User> findByEmail(String email) {
        return findByEmailIgnoreCase(email);
    }

    default Optional<User> findWithAuthoritiesById(Long id) {
        return findById(id);
    }

    default Optional<User> findWithAuthoritiesByEmail(String email) {
        return findByEmailIgnoreCase(email);
    }

    boolean existsByEmailIgnoreCase(String email);

    default boolean existsByEmail(String email) {
        return existsByEmailIgnoreCase(email);
    }
}
