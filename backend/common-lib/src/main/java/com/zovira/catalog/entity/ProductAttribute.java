package com.zovira.catalog.entity;

import com.zovira.common.entity.BaseEntity;
import java.util.ArrayList;
import java.util.List;

/** A selectable option dimension of a product (for example Color or Storage). */
public class ProductAttribute extends BaseEntity {

    @org.springframework.data.annotation.Transient
    private Product product;
    private Long productId;
    private String name;
    private int position;
    private List<AttributeOption> optionValues = new ArrayList<>();

    public ProductAttribute() {
    }

    public ProductAttribute(String name, int position, List<AttributeOption> optionValues) {
        this.name = name;
        this.position = position;
        this.optionValues = optionValues == null ? new ArrayList<>() : new ArrayList<>(optionValues);
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public List<AttributeOption> getOptionValues() {
        return optionValues;
    }

    public void setOptionValues(List<AttributeOption> optionValues) {
        this.optionValues = optionValues == null ? new ArrayList<>() : new ArrayList<>(optionValues);
    }
}
