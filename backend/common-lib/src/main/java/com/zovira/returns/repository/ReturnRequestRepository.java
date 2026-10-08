package com.zovira.returns.repository;

import com.zovira.returns.entity.ReturnRequest;
import com.zovira.returns.entity.ReturnStatus;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ReturnRequestRepository extends MongoRepository<ReturnRequest, Long> {

    Page<ReturnRequest> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<ReturnRequest> findBySellerIdAndStatusInOrderByCreatedAtDesc(Long sellerId, Collection<ReturnStatus> statuses,
            Pageable pageable);

    default Page<ReturnRequest> findForSeller(Long sellerId, Collection<ReturnStatus> statuses, Pageable pageable) {
        return findBySellerIdAndStatusInOrderByCreatedAtDesc(sellerId, statuses, pageable);
    }

    Page<ReturnRequest> findByStatusInOrderByCreatedAtDesc(Collection<ReturnStatus> statuses, Pageable pageable);

    default Page<ReturnRequest> findByStatuses(Collection<ReturnStatus> statuses, Pageable pageable) {
        return findByStatusInOrderByCreatedAtDesc(statuses, pageable);
    }

    long countBySellerIdAndStatus(Long sellerId, ReturnStatus status);

    long countByStatus(ReturnStatus status);
}
