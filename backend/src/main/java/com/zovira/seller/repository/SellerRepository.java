package com.zovira.seller.repository;

import com.zovira.seller.entity.Seller;
import com.zovira.seller.entity.SellerStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SellerRepository extends JpaRepository<Seller, Long>, JpaSpecificationExecutor<Seller> {

    Optional<Seller> findByUserId(Long userId);

    Optional<Seller> findBySlug(String slug);

    boolean existsBySlug(String slug);

    long countByStatus(SellerStatus status);
}
