package com.zovira.user.repository;

import com.zovira.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("select u from User u where lower(u.email) = lower(:email)")
    Optional<User> findByEmail(@Param("email") String email);

    /** Loads a user together with roles and permissions in one round trip (used when issuing tokens). */
    @Query("""
            select distinct u from User u
            left join fetch u.roles r
            left join fetch r.permissions
            where u.id = :id
            """)
    Optional<User> findWithAuthoritiesById(@Param("id") Long id);

    @Query("""
            select distinct u from User u
            left join fetch u.roles r
            left join fetch r.permissions
            where lower(u.email) = lower(:email)
            """)
    Optional<User> findWithAuthoritiesByEmail(@Param("email") String email);

    @Query("select count(u) > 0 from User u where lower(u.email) = lower(:email)")
    boolean existsByEmail(@Param("email") String email);
}
