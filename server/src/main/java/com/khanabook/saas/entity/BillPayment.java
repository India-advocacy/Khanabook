package com.khanabook.saas.entity;

import com.khanabook.saas.sync.entity.BaseSyncEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "bill_payments", indexes = {
    @Index(name = "idx_bill_payments_tenant_updated", columnList = "restaurant_id, updated_at"),
    @Index(name = "idx_bill_payments_device", columnList = "restaurant_id, device_id, local_id")
})
@Getter
@Setter
public class BillPayment extends BaseSyncEntity {

    @Column(name = "bill_id", nullable = true)
    private Integer billId;

    @Column(name = "payment_mode")
    private String paymentMode;

    @Column(name = "amount")
    private Double amount;
}
