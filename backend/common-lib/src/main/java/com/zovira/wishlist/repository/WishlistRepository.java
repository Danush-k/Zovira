package com.zovira.wishlist.repository;

import java.util.Optional;
import com.zovira.wishlist.entity.Wishlist;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface WishlistRepository extends MongoRepository<Wishlist, Long> {

    Optional<Wishlist> findByUserId(Long userId);
}
