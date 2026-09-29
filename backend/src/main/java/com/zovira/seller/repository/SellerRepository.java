package com.zovira.seller.repository;

import java.util.Optional;
import com.zovira.seller.entity.Seller;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerRepository extends JpaRepository<Seller, Long> {

    Optional<Seller> findByUserId(Long userId);

    Optional<Seller> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
