package com.khanabook.saas.sync.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface SyncRepository<T, ID> extends JpaRepository<T, ID> {
    // PULL Logic: Get newer records for this tenant, excluding the requesting device
    List<T> findByRestaurantIdAndUpdatedAtGreaterThanAndDeviceIdNot(Long restaurantId, Long lastSyncTimestamp, String deviceId);

    // Preferred pull watermark: use server timestamps to avoid client clock skew.
    List<T> findByRestaurantIdAndServerUpdatedAtGreaterThanAndDeviceIdNot(Long restaurantId, Long lastSyncTimestamp, String deviceId);
    
    // UPSERT Logic: Find exact record to update
    Optional<T> findByRestaurantIdAndDeviceIdAndLocalId(Long restaurantId, String deviceId, Integer localId);
    
    // ADD THIS NEW METHOD for batch fetching
    List<T> findByRestaurantIdAndDeviceIdAndLocalIdIn(Long restaurantId, String deviceId, List<Integer> localIds);

    // Cross-device fallback for singleton-style records using the same tenant/localId.
    List<T> findByRestaurantIdAndLocalIdIn(Long restaurantId, List<Integer> localIds);
}
