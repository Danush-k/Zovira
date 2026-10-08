package com.zovira.review.entity;

import com.zovira.common.entity.BaseEntity;

public class ReviewImage extends BaseEntity {

    private Review review;
    private Long reviewId;
    private String url;
    private int position;

    public ReviewImage() {
    }

    public ReviewImage(String url, int position) {
        this.url = url;
        this.position = position;
    }

    public void setReview(Review review) {
        this.review = review;
        this.reviewId = review != null ? review.getId() : null;
    }

    public Review getReview() {
        return review;
    }

    public Long getReviewId() {
        if (reviewId != null) return reviewId;
        return review != null ? review.getId() : null;
    }

    public void setReviewId(Long reviewId) {
        this.reviewId = reviewId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
