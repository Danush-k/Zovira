package com.zovira.order.entity;

import com.zovira.catalog.entity.Product;
import com.zovira.catalog.entity.ProductVariant;
import com.zovira.common.entity.BaseEntity;
import com.zovira.seller.entity.Seller;
import com.zovira.shipping.entity.Shipment;
import java.math.BigDecimal;
import java.time.Instant;

/** A purchased line. Title, price and image are snapshots taken at checkout. */
public class OrderItem extends BaseEntity {

    @org.springframework.data.annotation.Transient
    private Order order;
    private Long orderId;
    @org.springframework.data.annotation.Transient
    private Shipment shipment;
    private Long shipmentId;
    private Product product;
    private Long productId;
    private ProductVariant variant;
    private Long variantId;
    private Seller seller;
    private Long sellerId;
    private String productTitle;
    private String variantName;
    private String sku;
    private String imageUrl;
    private BigDecimal unitPrice;
    private BigDecimal unitMrp;
    private int quantity;
    private BigDecimal taxRate;
    private BigDecimal lineTotal;
    private BigDecimal couponDiscount = BigDecimal.ZERO;
    private OrderItemStatus status = OrderItemStatus.ACTIVE;
    private Instant createdAt = Instant.now();

    public OrderItem() {
    }

    public OrderItem(Product product, ProductVariant variant, Seller seller, String imageUrl, int quantity,
            BigDecimal taxRate, Instant createdAt) {
        this.product = product;
        this.productId = product != null ? product.getId() : null;
        this.variant = variant;
        this.variantId = variant != null ? variant.getId() : null;
        this.seller = seller;
        this.sellerId = seller != null ? seller.getId() : null;
        this.productTitle = product != null ? product.getTitle() : null;
        this.variantName = variant != null ? variant.getName() : null;
        this.sku = variant != null ? variant.getSku() : null;
        this.imageUrl = imageUrl;
        this.unitPrice = variant != null ? variant.getPrice() : BigDecimal.ZERO;
        this.unitMrp = variant != null ? variant.getMrp() : BigDecimal.ZERO;
        this.quantity = quantity;
        this.taxRate = taxRate;
        this.lineTotal = (this.unitPrice != null ? this.unitPrice : BigDecimal.ZERO).multiply(BigDecimal.valueOf(quantity));
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    /** Amount actually paid for this line after its share of the coupon. */
    public BigDecimal netAmount() {
        return lineTotal.subtract(couponDiscount != null ? couponDiscount : BigDecimal.ZERO);
    }

    public void setOrder(Order order) {
        this.order = order;
        this.orderId = order != null ? order.getId() : null;
    }

    public Order getOrder() {
        return order;
    }

    public Long getOrderId() {
        if (orderId != null) return orderId;
        return order != null ? order.getId() : null;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Shipment getShipment() {
        return shipment;
    }

    public void setShipment(Shipment shipment) {
        this.shipment = shipment;
        this.shipmentId = shipment != null ? shipment.getId() : null;
    }

    public Long getShipmentId() {
        if (shipmentId != null) return shipmentId;
        return shipment != null ? shipment.getId() : null;
    }

    public void setShipmentId(Long shipmentId) {
        this.shipmentId = shipmentId;
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

    public ProductVariant getVariant() {
        return variant;
    }

    public void setVariant(ProductVariant variant) {
        this.variant = variant;
        this.variantId = variant != null ? variant.getId() : null;
    }

    public Long getVariantId() {
        if (variantId != null) return variantId;
        return variant != null ? variant.getId() : null;
    }

    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }

    public Seller getSeller() {
        return seller;
    }

    public void setSeller(Seller seller) {
        this.seller = seller;
        this.sellerId = seller != null ? seller.getId() : null;
    }

    public Long getSellerId() {
        if (sellerId != null) return sellerId;
        return seller != null ? seller.getId() : null;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public String getProductTitle() {
        return productTitle;
    }

    public void setProductTitle(String productTitle) {
        this.productTitle = productTitle;
    }

    public String getVariantName() {
        return variantName;
    }

    public void setVariantName(String variantName) {
        this.variantName = variantName;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getUnitMrp() {
        return unitMrp;
    }

    public void setUnitMrp(BigDecimal unitMrp) {
        this.unitMrp = unitMrp;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
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

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
