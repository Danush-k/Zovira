package com.zovira.order.entity;

import com.zovira.catalog.entity.Product;
import com.zovira.catalog.entity.ProductVariant;
import com.zovira.common.entity.BaseEntity;
import com.zovira.seller.entity.Seller;
import com.zovira.shipping.entity.Shipment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/** A purchased line. Title, price and image are snapshots taken at checkout. */
@Entity
@Table(name = "order_items")
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id")
    private Shipment shipment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false, updatable = false)
    private ProductVariant variant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false, updatable = false)
    private Seller seller;

    @Column(nullable = false, length = 200)
    private String productTitle;

    @Column(length = 160)
    private String variantName;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(length = 512)
    private String imageUrl;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitMrp;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    /** Share of the order-level coupon discount allocated to this line (for accurate refunds). */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal couponDiscount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderItemStatus status = OrderItemStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected OrderItem() {
    }

    public OrderItem(Product product, ProductVariant variant, Seller seller, String imageUrl, int quantity,
            BigDecimal taxRate, Instant createdAt) {
        this.product = product;
        this.variant = variant;
        this.seller = seller;
        this.productTitle = product.getTitle();
        this.variantName = variant.getName();
        this.sku = variant.getSku();
        this.imageUrl = imageUrl;
        this.unitPrice = variant.getPrice();
        this.unitMrp = variant.getMrp();
        this.quantity = quantity;
        this.taxRate = taxRate;
        this.lineTotal = variant.getPrice().multiply(BigDecimal.valueOf(quantity));
        this.createdAt = createdAt;
    }

    /** Amount actually paid for this line after its share of the coupon. */
    public BigDecimal netAmount() {
        return lineTotal.subtract(couponDiscount);
    }

    void setOrder(Order order) {
        this.order = order;
    }

    public Order getOrder() {
        return order;
    }

    public Shipment getShipment() {
        return shipment;
    }

    public void setShipment(Shipment shipment) {
        this.shipment = shipment;
    }

    public Product getProduct() {
        return product;
    }

    public ProductVariant getVariant() {
        return variant;
    }

    public Seller getSeller() {
        return seller;
    }

    public String getProductTitle() {
        return productTitle;
    }

    public String getVariantName() {
        return variantName;
    }

    public String getSku() {
        return sku;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getUnitMrp() {
        return unitMrp;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public BigDecimal getCouponDiscount() {
        return couponDiscount;
    }

    public void setCouponDiscount(BigDecimal couponDiscount) {
        this.couponDiscount = couponDiscount;
    }

    public OrderItemStatus getStatus() {
        return status;
    }

    public void setStatus(OrderItemStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
