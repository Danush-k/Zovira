package com.zovira.catalog.entity;

import com.zovira.common.entity.AuditableEntity;
import com.zovira.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "product_questions")
public class ProductQuestion extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(nullable = false, length = 500)
    private String question;

    @Column(length = 1000)
    private String answer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answered_by")
    private User answeredBy;

    private Instant answeredAt;

    protected ProductQuestion() {
    }

    public ProductQuestion(Product product, User user, String question) {
        this.product = product;
        this.user = user;
        this.question = question;
    }

    public void answer(String answer, User answeredBy, Instant now) {
        this.answer = answer;
        this.answeredBy = answeredBy;
        this.answeredAt = now;
    }

    public Product getProduct() {
        return product;
    }

    public User getUser() {
        return user;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public User getAnsweredBy() {
        return answeredBy;
    }

    public Instant getAnsweredAt() {
        return answeredAt;
    }
}
