package com.zovira.shipping.repository;

import com.zovira.shipping.entity.Shipment;
import com.zovira.shipping.entity.ShipmentStatus;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ShipmentRepository extends MongoRepository<Shipment, Long> {

    Page<Shipment> findBySellerIdAndStatusInOrderByCreatedAtDesc(Long sellerId,
            Collection<ShipmentStatus> statuses, Pageable pageable);

    default Page<Shipment> findForSeller(Long sellerId, Collection<ShipmentStatus> statuses, Pageable pageable) {
        return findBySellerIdAndStatusInOrderByCreatedAtDesc(sellerId, statuses, pageable);
    }

    long countBySellerIdAndStatusIn(Long sellerId, Collection<ShipmentStatus> statuses);

    long countByStatusIn(Collection<ShipmentStatus> statuses);
}
