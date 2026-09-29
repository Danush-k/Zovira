package com.zovira.catalog.entity;

import com.zovira.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A selectable option dimension of a product (for example Color or Storage). */
@Entity
@Table(name = "product_attributes")
public class ProductAttribute extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Product product;

    @Column(nullable = false, length = 40)
    private String name;

    @Column(nullable = false)
    private int position;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<AttributeOption> optionValues = new ArrayList<>();

    protected ProductAttribute() {
    }

    public ProductAttribute(String name, int position, List<AttributeOption> optionValues) {
        this.name = name;
        this.position = position;
        this.optionValues = new ArrayList<>(optionValues);
    }

    void setProduct(Product product) {
        this.product = product;
    }

    public Product getProduct() {
        return product;
    }

    public String getName() {
        return name;
    }

    public int getPosition() {
        return position;
    }

    public List<AttributeOption> getOptionValues() {
        return optionValues;
    }
}
