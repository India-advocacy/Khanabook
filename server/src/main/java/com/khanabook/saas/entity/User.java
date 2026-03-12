package com.khanabook.saas.entity;

import com.khanabook.saas.sync.entity.BaseSyncEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_tenant_updated", columnList = "restaurant_id, updated_at"),
    @Index(name = "idx_users_device", columnList = "restaurant_id, device_id, local_id"),
    @Index(name = "idx_users_email", columnList = "email", unique = true)
})
@Getter
@Setter
public class User extends BaseSyncEntity {

    @Column(name = "name")
    private String name;

    @Column(name = "email")
    private String email;

    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY)
    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "whatsapp_number")
    private String whatsappNumber;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private String createdAt;
}
