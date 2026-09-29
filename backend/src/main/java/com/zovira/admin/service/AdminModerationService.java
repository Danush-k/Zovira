package com.zovira.admin.service;

import com.zovira.admin.dto.AdminOrderRow;
import com.zovira.admin.dto.AdminReviewRow;
import com.zovira.admin.dto.AuditLogRow;
import com.zovira.audit.repository.AuditLogRepository;
import com.zovira.audit.service.AuditService;
import com.zovira.common.exception.NotFoundException;
import com.zovira.common.web.PageRequests;
import com.zovira.common.web.PageResponse;
import com.zovira.order.entity.Order;
import com.zovira.order.entity.OrderStatus;
import com.zovira.order.repository.OrderRepository;
import com.zovira.order.service.OrderCommandService;
import com.zovira.review.entity.Review;
import com.zovira.review.entity.ReviewImage;
import com.zovira.review.entity.ReviewStatus;
import com.zovira.review.repository.ReviewRepository;
import com.zovira.review.service.ReviewRatingUpdater;
import java.time.Clock;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Review moderation, order oversight and the audit trail. */
@Service
public class AdminModerationService {

    private final ReviewRepository reviews;
    private final OrderRepository orders;
    private final AuditLogRepository auditLogs;
    private final OrderCommandService orderCommands;
    private final ReviewRatingUpdater ratingUpdater;
    private final AuditService audit;
    private final Clock clock;

    public AdminModerationService(ReviewRepository reviews, OrderRepository orders, AuditLogRepository auditLogs,
            OrderCommandService orderCommands, ReviewRatingUpdater ratingUpdater, AuditService audit, Clock clock) {
        this.reviews = reviews;
        this.orders = orders;
        this.auditLogs = auditLogs;
        this.orderCommands = orderCommands;
        this.ratingUpdater = ratingUpdater;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminReviewRow> listReviews(String status, Integer rating, Integer page, Integer size) {
        Specification<Review> spec = Specification.allOf();
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            ReviewStatus parsed = ReviewStatus.valueOf(status.toUpperCase(Locale.ROOT));
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), parsed));
        }
        if (rating != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("rating"), rating));
        }
        Page<Review> found = reviews.findAll(spec, PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.of(found, AdminModerationService::toRow);
    }

    @Transactional
    public AdminReviewRow moderateReview(Long adminId, Long reviewId, ReviewStatus status, String note) {
        Review review = reviews.findById(reviewId).orElseThrow(() -> NotFoundException.of("Review"));
        review.moderate(status, note, adminId, clock.instant());
        ratingUpdater.refresh(review.getProduct());
        audit.record("REVIEW_MODERATED", "REVIEW", reviewId, Map.of("status", status.name(), "note", String.valueOf(note)));
        return toRow(review);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminOrderRow> listOrders(String status, String query, Integer page, Integer size) {
        Specification<Order> spec = Specification.allOf();
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            OrderStatus parsed = OrderStatus.valueOf(status.toUpperCase(Locale.ROOT));
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), parsed));
        }
        if (query != null && !query.isBlank()) {
            String like = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, q, cb) -> cb.or(cb.like(cb.lower(root.get("orderNumber")), like),
                    cb.like(cb.lower(root.get("user").get("email")), like)));
        }
        Page<Order> found = orders.findAll(spec, PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "placedAt")));
        return PageResponse.of(found, o -> new AdminOrderRow(o.getOrderNumber(), o.getUser().getFullName(),
                o.getUser().getEmail(), o.getStatus().name(), o.getPaymentStatus().name(), o.getPaymentMethod().name(),
                o.getTotalAmount(), o.getRefundedAmount(), o.getItemCount(), o.getShippingAddress().getCity(),
                o.getPlacedAt()));
    }

    /** Administrative cancellation, for support cases the customer can no longer do themselves. */
    @Transactional
    public void cancelOrder(Long adminId, String orderNumber, String reason) {
        Order order = orders.findByOrderNumber(orderNumber).orElseThrow(() -> NotFoundException.of("Order"));
        orderCommands.cancel(order, "Cancelled by Zovira support: " + (reason == null ? "no reason given" : reason), adminId);
        audit.record("ORDER_CANCELLED_BY_ADMIN", "ORDER", orderNumber, Map.of("reason", String.valueOf(reason)));
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogRow> auditLog(Integer page, Integer size) {
        return PageResponse.of(auditLogs.findAll(PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))),
                a -> new AuditLogRow(a.getId(), a.getActorEmail(), a.getAction(), a.getEntityType(), a.getEntityId(),
                        a.getDetails(), a.getIpAddress(), a.getCreatedAt()));
    }

    private static AdminReviewRow toRow(Review r) {
        return new AdminReviewRow(r.getId(), r.getProduct().getId(), r.getProduct().getTitle(),
                r.getProduct().getSlug(), r.getProduct().primaryImageUrl(), r.getUser().getFullName(), r.getRating(),
                r.getTitle(), r.getBody(), r.getImages().stream().map(ReviewImage::getUrl).toList(),
                r.isVerifiedPurchase(), r.getStatus().name(), r.getModerationNote(), r.getHelpfulCount(),
                r.getCreatedAt());
    }
}
