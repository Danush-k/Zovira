package com.zovira.payment.repository;

import com.zovira.payment.entity.Refund;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RefundRepository extends MongoRepository<Refund, Long> {
}
