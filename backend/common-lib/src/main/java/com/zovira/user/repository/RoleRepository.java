package com.zovira.user.repository;

import java.util.Optional;
import com.zovira.user.entity.Role;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RoleRepository extends MongoRepository<Role, Long> {

    Optional<Role> findByName(String name);
}
