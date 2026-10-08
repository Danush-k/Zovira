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
import com.zovira.coupon.entity.CouponRedemption;
import com.zovira.coupon.entity.CouponScope;
import com.zovira.coupon.repository.CouponRepository;
import com.zovira.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminCouponService {

    private final CouponRepository coupons;
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final UserRepository users;
    private final MongoTemplate mongoTemplate;
    private final AuditService audit;
    private final Clock clock;

    public AdminCouponService(CouponRepository coupons, CategoryRepository categories, ProductRepository products,
            UserRepository users, MongoTemplate mongoTemplate, AuditService audit, Clock clock) {
        this.coupons = coupons;
        this.categories = categories;
        this.products = products;
        this.users = users;
        this.mongoTemplate = mongoTemplate;
        this.audit = audit;
        this.clock = clock;
    }

    public PageResponse<AdminCouponRow> list(Integer page, Integer size) {
        Page<Coupon> found = coupons.findAll(PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        Map<Long, BigDecimal> given = discountGiven(found.getContent().stream().map(Coupon::getId).toList());
        return PageResponse.of(found, c -> toRow(c, given.getOrDefault(c.getId(), BigDecimal.ZERO), clock));
    }

    public AdminCouponRow create(CouponRequest request) {
        String code = Coupon.normalizeCode(request.code());
        if (coupons.existsByCodeIgnoreCase(code)) {
            throw new BusinessException(ErrorCodes.CONFLICT, "Coupon code already exists");
        }
        Coupon coupon = new Coupon(code, request.description(), request.discountType(), request.discountValue(),
                request.startsAt());
        applyDetails(coupon, request);
        Coupon saved = coupons.save(coupon);
        audit.record("COUPON_CREATED", "COUPON", saved.getId(), Map.of("code", saved.getCode()));
        return toRow(saved, BigDecimal.ZERO, clock);
    }

    public AdminCouponRow update(Long id, CouponRequest request) {
        Coupon coupon = coupons.findById(id).orElseThrow(() -> NotFoundException.of("Coupon"));
        String code = Coupon.normalizeCode(request.code());
        if (!code.equals(coupon.getCode()) && coupons.existsByCodeIgnoreCase(code)) {
            throw new BusinessException(ErrorCodes.CONFLICT, "Coupon code already exists");
        }
        coupon.setCode(code);
        coupon.setDescription(request.description());
        coupon.setDiscountType(request.discountType());
        coupon.setDiscountValue(request.discountValue());
        coupon.setStartsAt(request.startsAt());
        applyDetails(coupon, request);
        Coupon saved = coupons.save(coupon);
        audit.record("COUPON_UPDATED", "COUPON", saved.getId(), Map.of("code", saved.getCode()));
        Map<Long, BigDecimal> given = discountGiven(List.of(saved.getId()));
        return toRow(saved, given.getOrDefault(saved.getId(), BigDecimal.ZERO), clock);
    }

    public AdminCouponRow setActive(Long id, boolean active) {
        Coupon coupon = coupons.findById(id).orElseThrow(() -> NotFoundException.of("Coupon"));
        coupon.setActive(active);
        Coupon saved = coupons.save(coupon);
        audit.record("COUPON_STATUS_CHANGED", "COUPON", saved.getId(), Map.of("active", active));
        Map<Long, BigDecimal> given = discountGiven(List.of(saved.getId()));
        return toRow(saved, given.getOrDefault(saved.getId(), BigDecimal.ZERO), clock);
    }

    private void applyDetails(Coupon coupon, CouponRequest request) {
        coupon.setMinOrderValue(request.minOrderValue() != null ? request.minOrderValue() : BigDecimal.ZERO);
        coupon.setMaxDiscount(request.maxDiscount());
        coupon.setExpiresAt(request.expiresAt());
        coupon.setUsageLimit(request.usageLimit());
        if (request.perUserLimit() > 0) {
            coupon.setPerUserLimit(request.perUserLimit());
        }
        coupon.setScope(request.scope() != null ? request.scope() : CouponScope.ALL);
        coupon.getCategories().clear();
        coupon.getProducts().clear();
        coupon.getUsers().clear();
        if (coupon.getScope() == CouponScope.CATEGORY && request.categoryIds() != null) {
            coupon.getCategories().addAll(categories.findAllById(request.categoryIds()));
        } else if (coupon.getScope() == CouponScope.PRODUCT && request.productIds() != null) {
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
        Query query = new Query(Criteria.where("couponId").in(ids));
        List<CouponRedemption> redemptions = mongoTemplate.find(query, CouponRedemption.class);
        Map<Long, BigDecimal> out = new HashMap<>();
        for (CouponRedemption r : redemptions) {
            if (r.getCouponId() != null && r.getDiscountAmount() != null) {
                out.put(r.getCouponId(), out.getOrDefault(r.getCouponId(), BigDecimal.ZERO).add(r.getDiscountAmount()));
            }
        }
        return out;
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
