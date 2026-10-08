package com.zovira.catalog.entity;

import com.zovira.common.entity.BaseEntity;

public class ProductSpecification extends BaseEntity {

    @org.springframework.data.annotation.Transient
    private Product product;
    private Long productId;
    private String groupName;
    private String name;
    private String value;
    private int position;

    public ProductSpecification() {
    }

    public ProductSpecification(String groupName, String name, String value, int position) {
        this.groupName = groupName;
        this.name = name;
        this.value = value;
        this.position = position;
    }

    public void setProduct(Product product) {
        this.product = product;
        if (product != null) {
            this.productId = product.getId();
        }
    }

    public Product getProduct() {
        return product;
    }

    public Long getProductId() {
        if (productId != null) return productId;
        return product != null ? product.getId() : null;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
