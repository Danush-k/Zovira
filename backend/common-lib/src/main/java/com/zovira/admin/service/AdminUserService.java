package com.zovira.admin.service;

import com.zovira.admin.dto.AdminSellerRow;
import com.zovira.admin.dto.AdminUserRow;
import com.zovira.audit.service.AuditService;
import com.zovira.auth.service.RefreshTokenService;
import com.zovira.catalog.entity.ProductStatus;
import com.zovira.catalog.repository.ProductRepository;
import com.zovira.common.exception.BusinessException;
import com.zovira.common.exception.ErrorCodes;
import com.zovira.common.exception.NotFoundException;
import com.zovira.mail.EmailTemplates;
import com.zovira.mail.MailService;
import com.zovira.common.web.PageRequests;
import com.zovira.common.web.PageResponse;
import com.zovira.order.entity.Order;
import com.zovira.order.entity.OrderItem;
import com.zovira.order.entity.OrderItemStatus;
import com.zovira.order.entity.OrderStatus;
import com.zovira.seller.entity.Seller;
import com.zovira.seller.entity.SellerStatus;
import com.zovira.seller.repository.SellerRepository;
import com.zovira.user.entity.Role;
import com.zovira.user.entity.User;
import com.zovira.user.entity.UserStatus;
import com.zovira.user.repository.RoleRepository;
import com.zovira.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Platform user operations: inspecting accounts, locking abuse, and vetting sellers. */
@Service
public class AdminUserService {

    private final UserRepository users;
    private final SellerRepository sellers;
    private final RoleRepository roles;
    private final ProductRepository products;
    private final RefreshTokenService refreshTokens;
    private final AuditService audit;
    private final MailService mail;
    private final MongoTemplate mongoTemplate;
    private final Clock clock;

    public AdminUserService(UserRepository users, SellerRepository sellers, RoleRepository roles,
            ProductRepository products, RefreshTokenService refreshTokens, AuditService audit, MailService mail,
            MongoTemplate mongoTemplate, Clock clock) {
        this.users = users;
        this.sellers = sellers;
        this.roles = roles;
        this.products = products;
        this.refreshTokens = refreshTokens;
        this.audit = audit;
        this.mail = mail;
        this.mongoTemplate = mongoTemplate;
        this.clock = clock;
    }

    public PageResponse<AdminUserRow> listUsers(String query, String role, String status, Integer page, Integer size) {
        Criteria criteria = new Criteria();
        List<Criteria> andList = new ArrayList<>();

        if (query != null && !query.isBlank()) {
            String q = query.trim();
            andList.add(new Criteria().orOperator(
                    Criteria.where("email").regex(q, "i"),
                    Criteria.where("fullName").regex(q, "i")
            ));
        }
        if (role != null && !role.isBlank() && !"ALL".equalsIgnoreCase(role)) {
            andList.add(Criteria.where("roles.name").is(role.toUpperCase(Locale.ROOT)));
        }
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            UserStatus parsed = UserStatus.valueOf(status.toUpperCase(Locale.ROOT));
            andList.add(Criteria.where("status").is(parsed));
        }
        if (!andList.isEmpty()) {
            criteria.andOperator(andList.toArray(new Criteria[0]));
        }

        Pageable pageable = PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        long total = mongoTemplate.count(new Query(criteria), User.class);
        List<User> userList = mongoTemplate.find(new Query(criteria).with(pageable), User.class);
        Page<User> found = new PageImpl<>(userList, pageable, total);

        Map<Long, Long> orderCounts = orderCounts(found.getContent().stream().map(User::getId).toList());
        return PageResponse.of(found, u -> new AdminUserRow(u.getId(), u.getEmail(), u.getFullName(), u.getPhone(),
                u.getStatus().name(), u.isEmailVerified(), u.getRoles().stream().map(Role::getName).sorted().toList(),
                orderCounts.getOrDefault(u.getId(), 0L), u.getLastLoginAt(), u.getCreatedAt()));
    }

    /** Suspending an account also revokes its sessions, so access ends immediately. */
    public AdminUserRow setUserStatus(Long adminId, Long userId, UserStatus status, String reason) {
        User user = users.findById(userId).orElseThrow(() -> NotFoundException.of("User"));
        if (user.getId().equals(adminId)) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "You can't change your own account status");
        }
        user.setStatus(status);
        if (status == UserStatus.SUSPENDED) {
            refreshTokens.revokeAll(userId, "ACCOUNT_SUSPENDED");
        }
        users.save(user);
        audit.record("USER_STATUS_CHANGED", "USER", userId, Map.of("status", status.name(), "reason", String.valueOf(reason)));
        return listUsers(user.getEmail(), null, null, 0, 1).content().getFirst();
    }

    public PageResponse<AdminSellerRow> listSellers(String status, String query, Integer page, Integer size) {
        Criteria criteria = new Criteria();
        List<Criteria> andList = new ArrayList<>();

        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            SellerStatus parsed = SellerStatus.valueOf(status.toUpperCase(Locale.ROOT));
            andList.add(Criteria.where("status").is(parsed));
        }
        if (query != null && !query.isBlank()) {
            andList.add(Criteria.where("storeName").regex(query.trim(), "i"));
        }
        if (!andList.isEmpty()) {
            criteria.andOperator(andList.toArray(new Criteria[0]));
        }

        Pageable pageable = PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        long total = mongoTemplate.count(new Query(criteria), Seller.class);
        List<Seller> sellerList = mongoTemplate.find(new Query(criteria).with(pageable), Seller.class);
        Page<Seller> found = new PageImpl<>(sellerList, pageable, total);

        Map<Long, BigDecimal> revenue = sellerRevenue(found.getContent().stream().map(Seller::getId).toList());
        return PageResponse.of(found, s -> new AdminSellerRow(s.getId(), s.getStoreName(), s.getSlug(),
                s.getUser() != null ? s.getUser().getFullName() : null,
                s.getUser() != null ? s.getUser().getEmail() : null,
                s.getGstin(), s.getPickupCity(), s.getPickupState(),
                s.getStatus().name(), s.getStatusReason(), s.getRatingAverage(), s.getRatingCount(),
                products.countBySellerIdAndStatus(s.getId(), ProductStatus.ACTIVE),
                revenue.getOrDefault(s.getId(), BigDecimal.ZERO), s.getCreatedAt(), s.getApprovedAt()));
    }

    /** Approving grants the SELLER role; rejecting or suspending removes it and unpublishes listings. */
    public AdminSellerRow decideSeller(Long sellerId, SellerStatus status, String reason) {
        Seller seller = sellers.findById(sellerId).orElseThrow(() -> NotFoundException.of("Seller"));
        User owner = seller.getUser();
        Role sellerRole = roles.findByName(Role.SELLER).orElseThrow();
        if (status == SellerStatus.APPROVED) {
            seller.approve(clock.instant());
            if (owner != null) {
                owner.getRoles().add(sellerRole);
                users.save(owner);
                mail.send(EmailTemplates.notice(owner.getEmail(), "Your Zovira seller account is approved",
                        "You're ready to sell", "Congratulations, " + owner.getFullName()
                                + ". Your store \"" + seller.getStoreName() + "\" is approved. Sign in to add products and start selling."));
            }
        } else {
            seller.changeStatus(status, reason);
            if (owner != null) {
                owner.getRoles().remove(sellerRole);
                users.save(owner);
            }
            products.findAll().stream()
                    .filter(p -> p.getSeller() != null && sellerId.equals(p.getSeller().getId()))
                    .forEach(p -> {
                        p.setStatus(ProductStatus.INACTIVE);
                        products.save(p);
                    });
            if (owner != null) {
                mail.send(EmailTemplates.notice(owner.getEmail(), "Update on your Zovira seller account",
                        "Account " + status.name().toLowerCase(Locale.ROOT),
                        "Your seller application or privileges have been " + status.name().toLowerCase(Locale.ROOT)
                                + (reason != null && !reason.isBlank() ? " for reason: " + reason : "")));
            }
        }
        sellers.save(seller);
        audit.record("SELLER_STATUS_CHANGED", "SELLER", sellerId,
                Map.of("status", status.name(), "reason", String.valueOf(reason)));
        return listSellers(null, seller.getStoreName(), 0, 1).content().getFirst();
    }

    private Map<Long, Long> orderCounts(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Query query = new Query(Criteria.where("userId").in(userIds));
        List<Order> orders = mongoTemplate.find(query, Order.class);
        Map<Long, Long> out = new HashMap<>();
        for (Order o : orders) {
            if (o.getUserId() != null) {
                out.put(o.getUserId(), out.getOrDefault(o.getUserId(), 0L) + 1);
            }
        }
        return out;
    }

    private Map<Long, BigDecimal> sellerRevenue(List<Long> sellerIds) {
        if (sellerIds.isEmpty()) {
            return Map.of();
        }
        List<OrderStatus> excludedStatuses = List.of(OrderStatus.CANCELLED, OrderStatus.PAYMENT_FAILED, OrderStatus.PENDING_PAYMENT);
        Query query = new Query(Criteria.where("status").nin(excludedStatuses));
        List<Order> orders = mongoTemplate.find(query, Order.class);
        Map<Long, BigDecimal> out = new HashMap<>();

        for (Order o : orders) {
            if (o.getItems() != null) {
                for (OrderItem item : o.getItems()) {
                    if (item.getSellerId() != null && sellerIds.contains(item.getSellerId()) && item.getStatus() != OrderItemStatus.CANCELLED) {
                        BigDecimal net = item.netAmount();
                        out.put(item.getSellerId(), out.getOrDefault(item.getSellerId(), BigDecimal.ZERO).add(net));
                    }
                }
            }
        }
        return out;
    }
}
