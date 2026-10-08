package com.zovira.seller.repository;

import com.zovira.seller.entity.Seller;
import com.zovira.seller.entity.SellerStatus;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SellerRepository extends MongoRepository<Seller, Long> {

    Optional<Seller> findByUserId(Long userId);

    Optional<Seller> findBySlug(String slug);

    boolean existsBySlug(String slug);

    long countByStatus(SellerStatus status);
}
