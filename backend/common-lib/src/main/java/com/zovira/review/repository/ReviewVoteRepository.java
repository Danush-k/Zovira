package com.zovira.review.repository;

import com.zovira.review.entity.ReviewVote;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ReviewVoteRepository extends MongoRepository<ReviewVote, ReviewVote.Id> {
}
