package com.zovira.returns.repository;

import com.zovira.returns.entity.ReturnRequest;
import com.zovira.returns.entity.ReturnStatus;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

    Page<ReturnRequest> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("select r from ReturnRequest r where r.seller.id = :sellerId and r.status in :statuses order by r.createdAt desc")
    Page<ReturnRequest> findForSeller(@Param("sellerId") Long sellerId, @Param("statuses") Collection<ReturnStatus> statuses,
            Pageable pageable);

    @Query("select r from ReturnRequest r where r.status in :statuses order by r.createdAt desc")
    Page<ReturnRequest> findByStatuses(@Param("statuses") Collection<ReturnStatus> statuses, Pageable pageable);

    long countBySellerIdAndStatus(Long sellerId, ReturnStatus status);

    long countByStatus(ReturnStatus status);
}
