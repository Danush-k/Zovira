package com.zovira.admin.service;

import com.zovira.admin.dto.AdminCouponRow;
import com.zovira.admin.dto.CouponRequest;
import com.zovira.audit.service.AuditService;
import com.zovira.catalog.entity.Category;
import com.zovira.catalog.entity.Product;
import com.zovira.catalog.repository.CategoryRepository;
import com.zovira.catalog.repository.ProductRepository;
import com.zovira.common.exception.BusinessException;
import com.zovira.common.exception.ErrorCodes;
import com.zovira.common.exception.NotFoundException;
import com.zovira.common.web.PageRequests;
import com.zovira.common.web.PageResponse;
import com.zovira.coupon.entity.Coupon;
import com.zovira.coupon.entity.CouponScope;
import com.zovira.coupon.entity.DiscountType;
import com.zovira.coupon.repository.CouponRepository;
import com.zovira.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminCouponService {

    private final CouponRepository coupons;
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final UserRepository users;
    private final NamedParameterJdbcTemplate jdbc;
    private final AuditService audit;
    private final Clock clock;

    public AdminCouponService(CouponRepository coupons, CategoryRepository categories, ProductRepository products,
            UserRepository users, NamedParameterJdbcTemplate jdbc, AuditService audit, Clock clock) {
        this.coupons = coupons;
        this.categories = categories;
        this.products = products;
        this.users = users;
        this.jdbc = jdbc;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminCouponRow> list(Integer page, Integer size) {
        Page<Coupon> found = coupons.findAll(PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        Map<Long, BigDecimal> given = discountGiven(found.getContent().stream().map(Coupon::getId).toList());
        return PageResponse.of(found, c -> toRow(c, given.getOrDefault(c.getId(), BigDecimal.ZERO), clock));
    }

    @Transactional
    public AdminCouponRow create(CouponRequest request) {
        if (coupons.existsByCodeIgnoreCase(request.code())) {
            throw new BusinessException(ErrorCodes.CONFLICT, "A coupon with this code already exists");
        }
        Coupon coupon = new Coupon(request.code(), request.description(), request.discountType(),
                request.discountValue(), request.startsAt() == null ? clock.instant() : request.startsAt());
        apply(coupon, request);
        coupons.save(coupon);
        audit.record("COUPON_CREATED", "COUPON", coupon.getId(), Map.of("code", coupon.getCode()));
        return toRow(coupon, BigDecimal.ZERO, clock);
    }

    @Transactional
    public AdminCouponRow update(Long id, CouponRequest request) {
        Coupon coupon = coupons.findById(id).orElseThrow(() -> NotFoundException.of("Coupon"));
        coupon.setDescription(request.description());
        coupon.setDiscountType(request.discountType());
        coupon.setDiscountValue(request.discountValue());
        if (request.startsAt() != null) {
            coupon.setStartsAt(request.startsAt());
        }
        apply(coupon, request);
        audit.record("COUPON_UPDATED", "COUPON", id, Map.of("code", coupon.getCode()));
        return toRow(coupon, discountGiven(List.of(id)).getOrDefault(id, BigDecimal.ZERO), clock);
    }

    @Transactional
    public AdminCouponRow setActive(Long id, boolean active) {
        Coupon coupon = coupons.findById(id).orElseThrow(() -> NotFoundException.of("Coupon"));
        coupon.setActive(active);
        audit.record("COUPON_STATUS_CHANGED", "COUPON", id, Map.of("active", active));
        return toRow(coupon, discountGiven(List.of(id)).getOrDefault(id, BigDecimal.ZERO), clock);
    }

    private void apply(Coupon coupon, CouponRequest request) {
        if (request.discountType() == DiscountType.PERCENTAGE
                && request.discountValue().compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new BusinessException(ErrorCodes.VALIDATION_FAILED, "Percentage discounts are capped at 90%");
        }
        if (request.expiresAt() != null && request.startsAt() != null
                && !request.expiresAt().isAfter(request.startsAt())) {
            throw new BusinessException(ErrorCodes.VALIDATION_FAILED, "The end date must be after the start date");
        }
        coupon.setMinOrderValue(request.minOrderValue() == null ? BigDecimal.ZERO : request.minOrderValue());
        coupon.setMaxDiscount(request.maxDiscount());
        coupon.setExpiresAt(request.expiresAt());
        coupon.setUsageLimit(request.usageLimit());
        coupon.setPerUserLimit(Math.max(1, request.perUserLimit()));
        coupon.setScope(request.scope());
        coupon.setActive(request.active());

        coupon.getCategories().clear();
        coupon.getProducts().clear();
        coupon.getUsers().clear();
        if (request.scope() == CouponScope.CATEGORY) {
            if (request.categoryIds() == null || request.categoryIds().isEmpty()) {
                throw new BusinessException(ErrorCodes.VALIDATION_FAILED, "Choose at least one category");
            }
            coupon.getCategories().addAll(categories.findAllById(request.categoryIds()));
        }
        if (request.scope() == CouponScope.PRODUCT) {
            if (request.productIds() == null || request.productIds().isEmpty()) {
                throw new BusinessException(ErrorCodes.VALIDATION_FAILED, "Choose at least one product");
            }
            coupon.getProducts().addAll(products.findAllById(request.productIds()));
        }
        boolean userSpecific = request.userIds() != null && !request.userIds().isEmpty();
        coupon.setUserSpecific(userSpecific);
        if (userSpecific) {
            coupon.getUsers().addAll(users.findAllById(request.userIds()));
        }
    }

    private Map<Long, BigDecimal> discountGiven(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return jdbc.query("SELECT coupon_id, COALESCE(SUM(discount_amount), 0) AS total FROM coupon_redemptions "
                + "WHERE coupon_id IN (:ids) GROUP BY coupon_id", new MapSqlParameterSource("ids", ids), rs -> {
                    Map<Long, BigDecimal> out = new HashMap<>();
                    while (rs.next()) {
                        out.put(rs.getLong("coupon_id"), rs.getBigDecimal("total"));
                    }
                    return out;
                });
    }

    static AdminCouponRow toRow(Coupon c, BigDecimal given, Clock clock) {
        return new AdminCouponRow(c.getId(), c.getCode(), c.getDescription(), c.getDiscountType().name(),
                c.getDiscountValue(), c.getMinOrderValue(), c.getMaxDiscount(), c.getStartsAt(), c.getExpiresAt(),
                c.getUsageLimit(), c.getPerUserLimit(), c.getUsedCount(), c.getScope().name(), c.isUserSpecific(),
                c.isActive(), c.isActive() && c.isWithinWindow(clock.instant()) && c.hasRemainingUses(), given,
                c.getCategories().stream().map(Category::getName).sorted().toList(),
                c.getProducts().stream().map(Product::getTitle).sorted().toList());
    }
}
