package com.zovira.admin.service;

import com.zovira.admin.dto.AdminOrderRow;
import com.zovira.admin.dto.AdminReviewRow;
import com.zovira.audit.repository.AuditLogRepository;
import com.zovira.audit.service.AuditService;
import com.zovira.catalog.entity.Product;
import com.zovira.common.exception.NotFoundException;
import com.zovira.common.web.PageRequests;
import com.zovira.common.web.PageResponse;
import com.zovira.admin.dto.AuditLogRow;
import com.zovira.audit.entity.AuditLog;
import com.zovira.order.service.OrderCommandService;
import com.zovira.order.entity.Order;
import com.zovira.order.entity.OrderStatus;
import com.zovira.order.repository.OrderRepository;
import com.zovira.review.entity.Review;
import com.zovira.review.entity.ReviewImage;
import com.zovira.review.entity.ReviewStatus;
import com.zovira.review.repository.ReviewRepository;
import com.zovira.review.service.ReviewRatingUpdater;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Moderation tools for customer-generated content and platform transactions. */
@Service
public class AdminModerationService {

    private final ReviewRepository reviews;
    private final OrderRepository orders;
    private final AuditLogRepository auditLogs;
    private final OrderCommandService orderCommands;
    private final ReviewRatingUpdater ratingUpdater;
    private final MongoTemplate mongoTemplate;
    private final AuditService audit;
    private final Clock clock;

    public AdminModerationService(ReviewRepository reviews, OrderRepository orders,
            AuditLogRepository auditLogs, OrderCommandService orderCommands, ReviewRatingUpdater ratingUpdater,
            MongoTemplate mongoTemplate, AuditService audit, Clock clock) {
        this.reviews = reviews;
        this.orders = orders;
        this.auditLogs = auditLogs;
        this.orderCommands = orderCommands;
        this.ratingUpdater = ratingUpdater;
        this.mongoTemplate = mongoTemplate;
        this.audit = audit;
        this.clock = clock;
    }

    public PageResponse<AdminReviewRow> listReviews(String status, Integer rating, Integer page, Integer size) {
        Criteria criteria = new Criteria();
        List<Criteria> andList = new ArrayList<>();

        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            ReviewStatus parsed = ReviewStatus.valueOf(status.toUpperCase(Locale.ROOT));
            andList.add(Criteria.where("status").is(parsed));
        }
        if (rating != null) {
            andList.add(Criteria.where("rating").is(rating));
        }
        if (!andList.isEmpty()) {
            criteria.andOperator(andList.toArray(new Criteria[0]));
        }

        Pageable pageable = PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        long total = mongoTemplate.count(new Query(criteria), Review.class);
        List<Review> list = mongoTemplate.find(new Query(criteria).with(pageable), Review.class);
        Page<Review> found = new PageImpl<>(list, pageable, total);

        return PageResponse.of(found, AdminModerationService::toRow);
    }

    public AdminReviewRow moderateReview(Long adminId, Long reviewId, ReviewStatus status, String note) {
        Review review = reviews.findById(reviewId).orElseThrow(() -> NotFoundException.of("Review"));
        review.moderate(status, note, adminId, clock.instant());
        reviews.save(review);
        ratingUpdater.refresh(review.getProduct());
        audit.record("REVIEW_MODERATED", "REVIEW", reviewId, Map.of("status", status.name(), "note", String.valueOf(note)));
        return toRow(review);
    }

    public PageResponse<AdminOrderRow> listOrders(String status, String query, Integer page, Integer size) {
        Criteria criteria = new Criteria();
        List<Criteria> andList = new ArrayList<>();

        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            OrderStatus parsed = OrderStatus.valueOf(status.toUpperCase(Locale.ROOT));
            andList.add(Criteria.where("status").is(parsed));
        }
        if (query != null && !query.isBlank()) {
            String q = query.trim();
            andList.add(new Criteria().orOperator(
                    Criteria.where("orderNumber").regex(q, "i"),
                    Criteria.where("user.email").regex(q, "i")
            ));
        }
        if (!andList.isEmpty()) {
            criteria.andOperator(andList.toArray(new Criteria[0]));
        }

        Pageable pageable = PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "placedAt"));
        long total = mongoTemplate.count(new Query(criteria), Order.class);
        List<Order> list = mongoTemplate.find(new Query(criteria).with(pageable), Order.class);
        Page<Order> found = new PageImpl<>(list, pageable, total);

        return PageResponse.of(found, o -> new AdminOrderRow(o.getOrderNumber(),
                o.getUser() != null ? o.getUser().getFullName() : null,
                o.getUser() != null ? o.getUser().getEmail() : null,
                o.getStatus().name(),
                o.getPaymentStatus() != null ? o.getPaymentStatus().name() : null,
                o.getPaymentMethod() != null ? o.getPaymentMethod().name() : null,
                o.getTotalAmount(), o.getRefundedAmount(), o.getItemCount(),
                o.getShippingAddress() != null ? o.getShippingAddress().getCity() : null,
                o.getPlacedAt()));
    }

    /** Administrative cancellation, for support cases the customer can no longer do themselves. */
    public void cancelOrder(Long adminId, String orderNumber, String reason) {
        Order order = orders.findByOrderNumber(orderNumber).orElseThrow(() -> NotFoundException.of("Order"));
        orderCommands.cancel(order, "Cancelled by Zovira support: " + (reason == null ? "no reason given" : reason), adminId);
        audit.record("ORDER_CANCELLED_BY_ADMIN", "ORDER", orderNumber, Map.of("reason", String.valueOf(reason)));
    }

    public PageResponse<AuditLogRow> auditLog(Integer page, Integer size) {
        Pageable pageable = PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLog> p = auditLogs.findAll(pageable);
        List<AuditLogRow> rows = p.getContent().stream()
                .map(a -> new AuditLogRow(a.getId(), a.getActorEmail(), a.getAction(), a.getEntityType(),
                        a.getEntityId(), a.getDetails(), a.getIpAddress(), a.getCreatedAt()))
                .toList();
        return PageResponse.of(rows, p.getNumber(), p.getSize(), p.getTotalElements());
    }

    static AdminReviewRow toRow(Review r) {
        Product p = r.getProduct();
        String imageUrl = p != null ? p.primaryImageUrl() : null;
        String customerName = r.getUser() != null ? r.getUser().getFullName() : null;
        List<String> images = r.getImages() != null
                ? r.getImages().stream().map(ReviewImage::getUrl).toList()
                : List.of();
        return new AdminReviewRow(
                r.getId(),
                p != null ? p.getId() : r.getProductId(),
                p != null ? p.getTitle() : null,
                p != null ? p.getSlug() : null,
                imageUrl,
                customerName,
                r.getRating(),
                r.getTitle(),
                r.getBody(),
                images,
                r.isVerifiedPurchase(),
                r.getStatus() != null ? r.getStatus().name() : null,
                r.getModerationNote(),
                r.getHelpfulCount(),
                r.getCreatedAt());
    }
}
