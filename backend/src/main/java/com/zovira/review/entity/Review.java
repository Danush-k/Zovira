package com.zovira.review.entity;

import com.zovira.catalog.entity.Product;
import com.zovira.common.entity.AuditableEntity;
import com.zovira.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reviews")
public class Review extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(nullable = false)
    private int rating;

    @Column(length = 120)
    private String title;

    @Column(length = 4000)
    private String body;

    @Column(nullable = false)
    private boolean verifiedPurchase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReviewStatus status = ReviewStatus.PUBLISHED;

    @Column(nullable = false)
    private int helpfulCount;

    @Column(nullable = false)
    private int notHelpfulCount;

    @Column(length = 500)
    private String moderationNote;

    private Long moderatedBy;

    private Instant moderatedAt;

    @Version
    private long version;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC, id ASC")
    private List<ReviewImage> images = new ArrayList<>();

    protected Review() {
    }

    public Review(Product product, User user, int rating, String title, String body, boolean verifiedPurchase,
            ReviewStatus status) {
        this.product = product;
        this.user = user;
        this.rating = rating;
        this.title = title;
        this.body = body;
        this.verifiedPurchase = verifiedPurchase;
        this.status = status;
    }

    public void edit(int rating, String title, String body) {
        this.rating = rating;
        this.title = title;
        this.body = body;
    }

    public void moderate(ReviewStatus status, String note, Long moderatorId, Instant now) {
        this.status = status;
        this.moderationNote = note;
        this.moderatedBy = moderatorId;
        this.moderatedAt = now;
    }

    public void applyVoteDelta(int helpfulDelta, int notHelpfulDelta) {
        this.helpfulCount = Math.max(0, helpfulCount + helpfulDelta);
        this.notHelpfulCount = Math.max(0, notHelpfulCount + notHelpfulDelta);
    }

    public void addImage(ReviewImage image) {
        image.setReview(this);
        images.add(image);
    }

    public Product getProduct() {
        return product;
    }

    public User getUser() {
        return user;
    }

    public int getRating() {
        return rating;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public boolean isVerifiedPurchase() {
        return verifiedPurchase;
    }

    public ReviewStatus getStatus() {
        return status;
    }

    public int getHelpfulCount() {
        return helpfulCount;
    }

    public int getNotHelpfulCount() {
        return notHelpfulCount;
    }

    public String getModerationNote() {
        return moderationNote;
    }

    public Long getModeratedBy() {
        return moderatedBy;
    }

    public Instant getModeratedAt() {
        return moderatedAt;
    }

    public List<ReviewImage> getImages() {
        return images;
    }
}
