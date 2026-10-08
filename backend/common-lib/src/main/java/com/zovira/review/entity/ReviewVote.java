package com.zovira.review.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.io.Serializable;
import java.time.Instant;

@Document(collection = "review_votes")
public class ReviewVote {

    public record Id(Long reviewId, Long userId) implements Serializable {
    }

    @org.springframework.data.annotation.Id
    private Id id;

    private boolean helpful;
    private Instant createdAt = Instant.now();

    public ReviewVote() {
    }

    public ReviewVote(Long reviewId, Long userId, boolean helpful, Instant createdAt) {
        this.id = new Id(reviewId, userId);
        this.helpful = helpful;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public Id getId() {
        return id;
    }

    public void setId(Id id) {
        this.id = id;
    }

    public boolean isHelpful() {
        return helpful;
    }

    public void setHelpful(boolean helpful) {
        this.helpful = helpful;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
