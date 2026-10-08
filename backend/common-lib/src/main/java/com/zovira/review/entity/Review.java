package com.zovira.review.entity;

import com.zovira.catalog.entity.Product;
import com.zovira.common.entity.AuditableEntity;
import com.zovira.user.entity.User;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "reviews")
public class Review extends AuditableEntity {

    private Product product;

    @Indexed
    private Long productId;

    private User user;

    @Indexed
    private Long userId;

    private int rating;
    private String title;
    private String body;
    private boolean verifiedPurchase;
    private ReviewStatus status = ReviewStatus.PUBLISHED;
    private int helpfulCount;
    private int notHelpfulCount;
    private String moderationNote;
    private Long moderatedBy;
    private Instant moderatedAt;

    @Version
    private Long version;

    private List<ReviewImage> images = new ArrayList<>();

    public Review() {
    }

    public Review(Product product, User user, int rating, String title, String body, boolean verifiedPurchase,
            ReviewStatus status) {
        this.product = product;
        this.productId = product != null ? product.getId() : null;
        this.user = user;
        this.userId = user != null ? user.getId() : null;
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
        if (image.getId() == null) {
            long maxId = images.stream().mapToLong(i -> i.getId() != null ? i.getId() : 0).max().orElse(0);
            image.setId(maxId + 1);
        }
        images.add(image);
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
        this.productId = product != null ? product.getId() : null;
    }

    public Long getProductId() {
        if (productId != null) return productId;
        return product != null ? product.getId() : null;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
        this.userId = user != null ? user.getId() : null;
    }

    public Long getUserId() {
        if (userId != null) return userId;
        return user != null ? user.getId() : null;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public boolean isVerifiedPurchase() {
        return verifiedPurchase;
    }

    public void setVerifiedPurchase(boolean verifiedPurchase) {
        this.verifiedPurchase = verifiedPurchase;
    }

    public ReviewStatus getStatus() {
        return status;
    }

    public void setStatus(ReviewStatus status) {
        this.status = status;
    }

    public int getHelpfulCount() {
        return helpfulCount;
    }

    public void setHelpfulCount(int helpfulCount) {
        this.helpfulCount = helpfulCount;
    }

    public int getNotHelpfulCount() {
        return notHelpfulCount;
    }

    public void setNotHelpfulCount(int notHelpfulCount) {
        this.notHelpfulCount = notHelpfulCount;
    }

    public String getModerationNote() {
        return moderationNote;
    }

    public void setModerationNote(String moderationNote) {
        this.moderationNote = moderationNote;
    }

    public Long getModeratedBy() {
        return moderatedBy;
    }

    public void setModeratedBy(Long moderatedBy) {
        this.moderatedBy = moderatedBy;
    }

    public Instant getModeratedAt() {
        return moderatedAt;
    }

    public void setModeratedAt(Instant moderatedAt) {
        this.moderatedAt = moderatedAt;
    }

    public List<ReviewImage> getImages() {
        return images;
    }

    public void setImages(List<ReviewImage> images) {
        this.images = images == null ? new ArrayList<>() : images;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
