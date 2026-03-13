package com.khanabook.saas.sync.service;

import com.khanabook.saas.sync.entity.BaseSyncEntity;
import com.khanabook.saas.sync.repository.SyncRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GenericSyncService {

    @Transactional
    public <T extends BaseSyncEntity> List<Integer> handlePushSync(
            Long tenantId,
            List<T> payload,
            SyncRepository<T, Long> repository) {

        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID (Restaurant ID) cannot be null. Ensure valid JWT is provided.");
        }

        if (payload == null || payload.isEmpty()) {
            return new ArrayList<>();
        }

        List<Integer> successfulLocalIds = new ArrayList<>();
        
        // 0. PRE-PROCESSING: Apply recovery logic to ensure localIds are populated
        // This MUST happen before we collect incomingLocalIds, otherwise they are omitted if null in JSON.
        for (T record : payload) {
            if (record.getLocalId() == null && record.getId() != null) {
                record.setLocalId(record.getId().intValue());
                record.setId(null); // Clear server ID for new insert
            }
        }

        // 1. Extract all localIds from the incoming payload
        List<Integer> incomingLocalIds = payload.stream()
                .map(BaseSyncEntity::getLocalId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // Assuming all records in a single payload come from the same device
        String deviceId = payload.get(0).getDeviceId();
        long serverTime = System.currentTimeMillis();

        // 2. Fetch all existing records from the DB in ONE query (matching Tenant + EXACT Device + Local IDs)
        List<T> existingRecords = repository.findByRestaurantIdAndDeviceIdAndLocalIdIn(
                tenantId, deviceId, incomingLocalIds);

        // EXTRA FALLBACK FOR SINGLETONS (Restaurant Profiles):
        // If the payload is RestaurantProfile and the exact device check missed,
        // it means the user re-installed the app on another device but is editing the same store.
        // Prevent "Duplicate Key Violations" by fetching ALL records for this tenant across devices to force an UPDATE.
        if (payload.get(0).getClass().getSimpleName().equals("RestaurantProfile")) {
            List<T> allTenantRecords = repository.findByRestaurantIdAndUpdatedAtGreaterThanAndDeviceIdNot(tenantId, 0L, "UNKNOWN_IMPOSSIBLE_DEVICE");
            for(T t : allTenantRecords) {
                boolean alreadyMatched = existingRecords.stream().anyMatch(e -> e.getLocalId().equals(t.getLocalId()) && e.getDeviceId().equals(t.getDeviceId()));
                if(!alreadyMatched) {
                    existingRecords.add(t);
                }
            }
        }

        // 3. Map existing records by localId AND deviceId for absolute uniqueness to determine if it's an Update
        // BUT for Singletons we merge by localId first to overwrite the old profile
        // Here we just map by localId to force UI overwrites for the same Restaurant
        Map<Integer, T> existingRecordMap = existingRecords.stream()
                .collect(Collectors.toMap(
                        BaseSyncEntity::getLocalId,
                        Function.identity(),
                        (existing, replacement) -> existing.getUpdatedAt() > replacement.getUpdatedAt() ? existing : replacement
                ));

        // Use a map for records to save to prevent multiple INSERT/UPDATE of same localId in one batch
        Map<Integer, T> recordsToSaveMap = new java.util.HashMap<>();

        // 4. Process the payload in memory
        for (T incomingRecord : payload) {
            try {
                if (incomingRecord.getLocalId() == null) {
                    // Fallback to localId=1 for singletons like Restaurant Profile to guarantee smooth merge
                    if (incomingRecord.getClass().getSimpleName().equals("RestaurantProfile")) {
                        incomingRecord.setLocalId(1);
                    } else {
                        System.err.println("[GenericSyncService] Skipping record with NULL localId!");
                        continue;
                    }
                }

                incomingRecord.setRestaurantId(tenantId);
                incomingRecord.setServerUpdatedAt(serverTime); // Enforce Server Time

                T existingRecord = existingRecordMap.get(incomingRecord.getLocalId());

                if (existingRecord != null) {
                    // It exists, check if we need to update
                    
                    // IF it exists but belongs to a DIFFERENT device (e.g. they changed phones or wiped data)
                    // We must OVERWRITE the old record rather than creating a new one with the same uniqueness constraints!
                    if (!existingRecord.getDeviceId().equals(incomingRecord.getDeviceId())) {
                         incomingRecord.setId(existingRecord.getId()); // Grab the real Server DB ID
                         incomingRecord.setDeviceId(existingRecord.getDeviceId()); // Keep original device ID to satisfy Constraint OR change DB to allow
                    } else {
                         incomingRecord.setId(existingRecord.getId());
                    }
                    
                    if (incomingRecord.getUpdatedAt() > existingRecord.getUpdatedAt()) {
                        T staged = recordsToSaveMap.get(incomingRecord.getLocalId());
                        if (staged == null || incomingRecord.getUpdatedAt() > staged.getUpdatedAt()) {
                            recordsToSaveMap.put(incomingRecord.getLocalId(), incomingRecord);
                        }
                        successfulLocalIds.add(incomingRecord.getLocalId());
                    } else if (!existingRecord.getDeviceId().equals(incomingRecord.getDeviceId())) {
                         // Force update if crossing devices to merge latest state
                         T staged = recordsToSaveMap.get(incomingRecord.getLocalId());
                         if (staged == null || incomingRecord.getUpdatedAt() > staged.getUpdatedAt()) {
                             recordsToSaveMap.put(incomingRecord.getLocalId(), incomingRecord);
                         }
                         successfulLocalIds.add(incomingRecord.getLocalId());
                    }
                } else {
                    // It doesn't exist, insert new
                    T staged = recordsToSaveMap.get(incomingRecord.getLocalId());
                    if (staged == null || incomingRecord.getUpdatedAt() > staged.getUpdatedAt()) {
                        recordsToSaveMap.put(incomingRecord.getLocalId(), incomingRecord);
                    }
                    successfulLocalIds.add(incomingRecord.getLocalId());
                }
            } catch (Exception e) {
                System.err.println("Sync Error for device " + incomingRecord.getDeviceId() + " : " + e.getMessage());
            }
        }

        // 5. Save all records to the DB in a single batch
        if (!recordsToSaveMap.isEmpty()) {
            repository.saveAll(recordsToSaveMap.values());
        }

        System.out.println("\n[GenericSyncService] Successfully batch synced " + successfulLocalIds.size()
                + " records for Tenant ID: " + tenantId);
        
        return successfulLocalIds;
    }
}
