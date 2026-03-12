package com.khanabook.saas.entity;

import com.khanabook.saas.sync.entity.BaseSyncEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "bill_items", indexes = {
    @Index(name = "idx_bill_items_tenant_updated", columnList = "restaurant_id, updated_at"),
    @Index(name = "idx_bill_items_device", columnList = "restaurant_id, device_id, local_id")
})
@Getter
@Setter
public class BillItem extends BaseSyncEntity {

    @Column(name = "bill_id", nullable = true)
    private Integer billId;

    @Column(name = "server_bill_id", nullable = true)
    private Long serverBillId;

    @Column(name = "menu_item_id", nullable = true)
    private Integer menuItemId;

    @Column(name = "server_menu_item_id", nullable = true)
    private Long serverMenuItemId;

    @Column(name = "item_name")
    private String itemName;

    @Column(name = "variant_id", nullable = true)
    private Integer variantId;

    @Column(name = "server_variant_id", nullable = true)
    private Long serverVariantId;

    @Column(name = "variant_name")
    private String variantName;

    @Column(name = "price")
    private Double price;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "item_total")
    private Double itemTotal;

    @Column(name = "special_instruction")
    private String specialInstruction;
}
