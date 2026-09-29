package com.zovira.catalog.entity;

import com.zovira.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_specifications")
public class ProductSpecification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Product product;

    @Column(nullable = false, length = 60)
    private String groupName;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 500)
    private String value;

    @Column(nullable = false)
    private int position;

    protected ProductSpecification() {
    }

    public ProductSpecification(String groupName, String name, String value, int position) {
        this.groupName = groupName;
        this.name = name;
        this.value = value;
        this.position = position;
    }

    void setProduct(Product product) {
        this.product = product;
    }

    public Product getProduct() {
        return product;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public int getPosition() {
        return position;
    }
}
