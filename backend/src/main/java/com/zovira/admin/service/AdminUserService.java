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
import com.zovira.common.web.PageRequests;
import com.zovira.common.web.PageResponse;
import com.zovira.mail.EmailTemplates;
import com.zovira.mail.MailService;
import com.zovira.seller.entity.Seller;
import com.zovira.seller.entity.SellerStatus;
import com.zovira.seller.repository.SellerRepository;
import com.zovira.user.entity.Role;
import com.zovira.user.entity.User;
import com.zovira.user.entity.UserStatus;
import com.zovira.user.repository.RoleRepository;
import com.zovira.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Admin management of customer accounts and seller applications. */
@Service
public class AdminUserService {

    private final UserRepository users;
    private final RoleRepository roles;
    private final SellerRepository sellers;
    private final ProductRepository products;
    private final RefreshTokenService refreshTokens;
    private final AuditService audit;
    private final MailService mail;
    private final NamedParameterJdbcTemplate jdbc;
    private final Clock clock;

    public AdminUserService(UserRepository users, RoleRepository roles, SellerRepository sellers,
            ProductRepository products, RefreshTokenService refreshTokens, AuditService audit, MailService mail,
            NamedParameterJdbcTemplate jdbc, Clock clock) {
        this.users = users;
        this.roles = roles;
        this.sellers = sellers;
        this.products = products;
        this.refreshTokens = refreshTokens;
        this.audit = audit;
        this.mail = mail;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserRow> listUsers(String query, String role, String status, Integer page, Integer size) {
        Specification<User> spec = Specification.allOf();
        if (query != null && !query.isBlank()) {
            String like = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, q, cb) -> cb.or(cb.like(cb.lower(root.get("email")), like),
                    cb.like(cb.lower(root.get("fullName")), like)));
        }
        if (role != null && !role.isBlank() && !"ALL".equalsIgnoreCase(role)) {
            spec = spec.and((root, q, cb) -> {
                q.distinct(true);
                return cb.equal(root.join("roles").get("name"), role.toUpperCase(Locale.ROOT));
            });
        }
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            UserStatus parsed = UserStatus.valueOf(status.toUpperCase(Locale.ROOT));
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), parsed));
        }
        Page<User> found = users.findAll(spec, PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        Map<Long, Long> orderCounts = orderCounts(found.getContent().stream().map(User::getId).toList());
        return PageResponse.of(found, u -> new AdminUserRow(u.getId(), u.getEmail(), u.getFullName(), u.getPhone(),
                u.getStatus().name(), u.isEmailVerified(), u.getRoles().stream().map(Role::getName).sorted().toList(),
                orderCounts.getOrDefault(u.getId(), 0L), u.getLastLoginAt(), u.getCreatedAt()));
    }

    /** Suspending an account also revokes its sessions, so access ends immediately. */
    @Transactional
    public AdminUserRow setUserStatus(Long adminId, Long userId, UserStatus status, String reason) {
        User user = users.findById(userId).orElseThrow(() -> NotFoundException.of("User"));
        if (user.getId().equals(adminId)) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "You can't change your own account status");
        }
        if (user.hasRole(Role.ADMIN) && status == UserStatus.SUSPENDED) {
            throw new BusinessException(ErrorCodes.FORBIDDEN, "Administrator accounts can't be suspended here");
        }
        user.setStatus(status);
        if (status == UserStatus.SUSPENDED) {
            refreshTokens.revokeAll(userId, "ACCOUNT_SUSPENDED");
        }
        audit.record("USER_STATUS_CHANGED", "USER", userId, Map.of("status", status.name(), "reason", String.valueOf(reason)));
        return listUsers(user.getEmail(), null, null, 0, 1).content().getFirst();
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminSellerRow> listSellers(String status, String query, Integer page, Integer size) {
        Specification<Seller> spec = Specification.allOf();
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            SellerStatus parsed = SellerStatus.valueOf(status.toUpperCase(Locale.ROOT));
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), parsed));
        }
        if (query != null && !query.isBlank()) {
            String like = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("storeName")), like));
        }
        Page<Seller> found = sellers.findAll(spec, PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        Map<Long, BigDecimal> revenue = sellerRevenue(found.getContent().stream().map(Seller::getId).toList());
        return PageResponse.of(found, s -> new AdminSellerRow(s.getId(), s.getStoreName(), s.getSlug(),
                s.getUser().getFullName(), s.getUser().getEmail(), s.getGstin(), s.getPickupCity(), s.getPickupState(),
                s.getStatus().name(), s.getStatusReason(), s.getRatingAverage(), s.getRatingCount(),
                products.countBySellerIdAndStatus(s.getId(), ProductStatus.ACTIVE),
                revenue.getOrDefault(s.getId(), BigDecimal.ZERO), s.getCreatedAt(), s.getApprovedAt()));
    }

    /** Approving grants the SELLER role; rejecting or suspending removes it and unpublishes listings. */
    @Transactional
    public AdminSellerRow decideSeller(Long sellerId, SellerStatus status, String reason) {
        Seller seller = sellers.findById(sellerId).orElseThrow(() -> NotFoundException.of("Seller"));
        User owner = seller.getUser();
        Role sellerRole = roles.findByName(Role.SELLER).orElseThrow();
        if (status == SellerStatus.APPROVED) {
            seller.approve(clock.instant());
            owner.getRoles().add(sellerRole);
            mail.send(EmailTemplates.notice(owner.getEmail(), "Your Zovira seller account is approved",
                    "You're ready to sell", "Congratulations, " + owner.getFullName()
                            + ". Your store \"" + seller.getStoreName() + "\" is approved. Sign in to add products and start selling."));
        } else {
            seller.changeStatus(status, reason);
            owner.getRoles().removeIf(r -> r.getName().equals(Role.SELLER));
            refreshTokens.revokeAll(owner.getId(), "SELLER_STATUS_CHANGED");
            jdbc.update("UPDATE products SET status = 'INACTIVE' WHERE seller_id = :id AND status = 'ACTIVE'",
                    new MapSqlParameterSource("id", sellerId));
            mail.send(EmailTemplates.notice(owner.getEmail(), "Update on your Zovira seller account",
                    status == SellerStatus.REJECTED ? "Application not approved" : "Seller account suspended",
                    reason == null || reason.isBlank()
                            ? "Contact Zovira support for details about this decision."
                            : reason));
        }
        audit.record("SELLER_STATUS_CHANGED", "SELLER", sellerId,
                Map.of("status", status.name(), "reason", String.valueOf(reason)));
        return listSellers(null, seller.getStoreName(), 0, 1).content().getFirst();
    }

    private Map<Long, Long> orderCounts(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return jdbc.query("SELECT user_id, COUNT(*) AS n FROM orders WHERE user_id IN (:ids) GROUP BY user_id",
                new MapSqlParameterSource("ids", userIds),
                rs -> {
                    Map<Long, Long> out = new java.util.HashMap<>();
                    while (rs.next()) {
                        out.put(rs.getLong("user_id"), rs.getLong("n"));
                    }
                    return out;
                });
    }

    private Map<Long, BigDecimal> sellerRevenue(List<Long> sellerIds) {
        if (sellerIds.isEmpty()) {
            return Map.of();
        }
        return jdbc.query("""
                SELECT oi.seller_id, COALESCE(SUM(oi.line_total - oi.coupon_discount), 0) AS revenue
                  FROM order_items oi JOIN orders o ON o.id = oi.order_id
                 WHERE oi.seller_id IN (:ids) AND oi.status <> 'CANCELLED'
                   AND o.status NOT IN ('CANCELLED', 'PAYMENT_FAILED', 'PENDING_PAYMENT')
                 GROUP BY oi.seller_id
                """, new MapSqlParameterSource("ids", sellerIds), rs -> {
            Map<Long, BigDecimal> out = new java.util.HashMap<>();
            while (rs.next()) {
                out.put(rs.getLong("seller_id"), rs.getBigDecimal("revenue"));
            }
            return out;
        });
    }
}
