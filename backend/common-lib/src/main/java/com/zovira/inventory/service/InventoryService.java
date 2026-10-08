package com.zovira.inventory.service;

import com.zovira.catalog.repository.ProductRepository;
import com.zovira.common.exception.BusinessException;
import com.zovira.common.exception.ErrorCodes;
import com.zovira.inventory.repository.InventoryRepository;
import com.zovira.order.entity.OrderItem;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stock lifecycle for orders: reserve on placement, commit when paid (or COD confirmed), release
 * if payment never arrives, restock on cancellation or return. Every mutation is a single
 * conditional UPDATE, so concurrent checkouts cannot oversell.
 */
@Service
public class InventoryService {

    private final InventoryRepository inventory;
    private final ProductRepository products;

    public InventoryService(InventoryRepository inventory, ProductRepository products) {
        this.inventory = inventory;
        this.products = products;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void reserve(List<OrderItem> items) {
        for (OrderItem item : items) {
            if (inventory.reserve(item.getVariant().getId(), item.getQuantity()) == 0) {
                throw new BusinessException(ErrorCodes.OUT_OF_STOCK,
                        item.getProductTitle() + " just sold out. Please review your cart.");
            }
        }
        refresh(items);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void commit(Collection<OrderItem> items) {
        items.forEach(i -> {
            inventory.commitReservation(i.getVariant().getId(), i.getQuantity());
            products.adjustSoldCount(i.getProduct().getId(), i.getQuantity());
        });
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void release(Collection<OrderItem> items) {
        items.forEach(i -> inventory.releaseReservation(i.getVariant().getId(), i.getQuantity()));
        refresh(items);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void restock(OrderItem item, int quantity) {
        inventory.restock(item.getVariant().getId(), quantity);
        products.adjustSoldCount(item.getProduct().getId(), -quantity);
        refresh(List.of(item));
    }

    private void refresh(Collection<OrderItem> items) {
        products.refreshTotalStock(items.stream().map(i -> i.getProduct().getId()).distinct().toList());
    }
}
