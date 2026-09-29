package com.zovira.review.entity;

import com.zovira.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "review_images")
public class ReviewImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false, updatable = false)
    private Review review;

    @Column(nullable = false, length = 512)
    private String url;

    @Column(nullable = false)
    private int position;

    protected ReviewImage() {
    }

    public ReviewImage(String url, int position) {
        this.url = url;
        this.position = position;
    }

    void setReview(Review review) {
        this.review = review;
    }

    public Review getReview() {
        return review;
    }

    public String getUrl() {
        return url;
    }

    public int getPosition() {
        return position;
    }
}
