package com.zovira.catalog.entity;

import com.zovira.common.entity.AuditableEntity;
import com.zovira.user.entity.User;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection = "product_questions")
public class ProductQuestion extends AuditableEntity {

    private Product product;

    @Indexed
    private Long productId;

    private User user;

    @Indexed
    private Long userId;

    private String question;
    private String answer;
    private User answeredBy;
    private Long answeredById;
    private Instant answeredAt;

    public ProductQuestion() {
    }

    public ProductQuestion(Product product, User user, String question) {
        this.product = product;
        this.productId = product != null ? product.getId() : null;
        this.user = user;
        this.userId = user != null ? user.getId() : null;
        this.question = question;
    }

    public void answer(String answer, User answeredBy, Instant now) {
        this.answer = answer;
        this.answeredBy = answeredBy;
        this.answeredById = answeredBy != null ? answeredBy.getId() : null;
        this.answeredAt = now;
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

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public User getAnsweredBy() {
        return answeredBy;
    }

    public void setAnsweredBy(User answeredBy) {
        this.answeredBy = answeredBy;
        this.answeredById = answeredBy != null ? answeredBy.getId() : null;
    }

    public Long getAnsweredById() {
        if (answeredById != null) return answeredById;
        return answeredBy != null ? answeredBy.getId() : null;
    }

    public void setAnsweredById(Long answeredById) {
        this.answeredById = answeredById;
    }

    public Instant getAnsweredAt() {
        return answeredAt;
    }

    public void setAnsweredAt(Instant answeredAt) {
        this.answeredAt = answeredAt;
    }
}
