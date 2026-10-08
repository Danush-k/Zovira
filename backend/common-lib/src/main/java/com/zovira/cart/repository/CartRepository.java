package com.zovira.cart.repository;

import java.util.Optional;
import com.zovira.cart.entity.Cart;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CartRepository extends MongoRepository<Cart, Long> {

    Optional<Cart> findByUserId(Long userId);
}
