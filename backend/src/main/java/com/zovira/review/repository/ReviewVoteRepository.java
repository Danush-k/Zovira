package com.zovira.review.repository;

import com.zovira.review.entity.ReviewVote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewVoteRepository extends JpaRepository<ReviewVote, ReviewVote.Id> {
}
