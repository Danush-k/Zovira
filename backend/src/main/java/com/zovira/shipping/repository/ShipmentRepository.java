package com.zovira.shipping.repository;

import com.zovira.shipping.entity.Shipment;
import com.zovira.shipping.entity.ShipmentStatus;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    @Query("""
            select s from Shipment s
            join fetch s.order o
            where s.seller.id = :sellerId and s.status in :statuses
            order by s.createdAt desc
            """)
    Page<Shipment> findForSeller(@Param("sellerId") Long sellerId,
            @Param("statuses") Collection<ShipmentStatus> statuses, Pageable pageable);

    long countBySellerIdAndStatusIn(Long sellerId, Collection<ShipmentStatus> statuses);

    long countByStatusIn(Collection<ShipmentStatus> statuses);
}
