package com.zovira.review.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "review_votes")
public class ReviewVote {

    @Embeddable
    public record Id(@Column(name = "review_id") Long reviewId, @Column(name = "user_id") Long userId)
            implements Serializable {
    }

    @EmbeddedId
    private Id id;

    @Column(nullable = false)
    private boolean helpful;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected ReviewVote() {
    }

    public ReviewVote(Long reviewId, Long userId, boolean helpful, Instant createdAt) {
        this.id = new Id(reviewId, userId);
        this.helpful = helpful;
        this.createdAt = createdAt;
    }

    public Id getId() {
        return id;
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
}
