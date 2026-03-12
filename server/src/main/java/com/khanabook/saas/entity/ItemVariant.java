package com.khanabook.saas.entity;

import com.khanabook.saas.sync.entity.BaseSyncEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "itemvariants", indexes = {
    @Index(name = "idx_itemvariants_tenant_updated", columnList = "restaurant_id, updated_at"),
    @Index(name = "idx_itemvariants_device", columnList = "restaurant_id, device_id, local_id")
})
@Getter
@Setter
public class ItemVariant extends BaseSyncEntity {

    @Column(name = "menu_item_id")
    private Integer menuItemId;

    @Column(name = "variant_name")
    private String variantName;

    @Column(name = "price")
    private Double price;

    @Column(name = "is_available")
    private Boolean isAvailable;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "current_stock")
    private Double currentStock;

    @Column(name = "low_stock_threshold")
    private Double lowStockThreshold;

    public MenuItem.StockStatus getStockStatus() {
        if (currentStock == null || currentStock <= 0) {
            return MenuItem.StockStatus.OUT_OF_STOCK;
        }
        if (lowStockThreshold != null && currentStock <= lowStockThreshold) {
            return MenuItem.StockStatus.RUNNING_LOW;
        }
        return MenuItem.StockStatus.IN_STOCK;
    }
}
