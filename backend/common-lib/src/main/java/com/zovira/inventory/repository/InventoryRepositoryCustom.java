package com.zovira.inventory.repository;

public interface InventoryRepositoryCustom {

    int reserve(Long variantId, int qty);

    int commitReservation(Long variantId, int qty);

    int releaseReservation(Long variantId, int qty);

    int restock(Long variantId, int qty);

    int sumAvailableForProduct(Long productId);
}
