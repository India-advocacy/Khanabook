package com.khanabook.saas.sync.service;

import com.khanabook.saas.entity.RestaurantProfile;
import com.khanabook.saas.entity.User;
import com.khanabook.saas.sync.dto.PushSyncResponse;
import com.khanabook.saas.sync.entity.BaseSyncEntity;
import com.khanabook.saas.sync.repository.SyncRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GenericSyncService {
    private static final Logger log = LoggerFactory.getLogger(GenericSyncService.class);

    @Transactional
    public <T extends BaseSyncEntity> PushSyncResponse handlePushSync(
            Long tenantId,
            List<T> payload,
            SyncRepository<T, Long> repository) {

        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID (Restaurant ID) cannot be null. Ensure valid JWT is provided.");
        }

        if (payload == null || payload.isEmpty()) {
            return new PushSyncResponse(new ArrayList<>(), new ArrayList<>());
        }

        List<Integer> successfulLocalIds = new ArrayList<>();
        List<Integer> failedLocalIds = new ArrayList<>();
        
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

        BaseSyncEntity firstRecord = payload.get(0);
        String deviceId = firstRecord.getDeviceId();
        long serverTime = System.currentTimeMillis();
        
        // CRIT-05 fix: Only certain entities should allow singleton-style tenant-level matching
        boolean isSingletonType = firstRecord instanceof RestaurantProfile || firstRecord instanceof User;
        boolean singletonStylePayload = isSingletonType &&
                payload.size() == 1 && (incomingLocalIds.isEmpty() || incomingLocalIds.contains(1));

        // 2. Fetch all existing records from the DB in ONE query (matching Tenant + EXACT Device + Local IDs)
        List<T> existingRecords = repository.findByRestaurantIdAndDeviceIdAndLocalIdIn(
                tenantId, deviceId, incomingLocalIds);

        // Singleton-style records use localId=1 across reinstalls, so allow a tenant+localId fallback.
        if (singletonStylePayload) {
            List<T> crossDeviceRecords = repository.findByRestaurantIdAndLocalIdIn(tenantId, List.of(1));
            for (T record : crossDeviceRecords) {
                boolean alreadyMatched = existingRecords.stream()
                        .anyMatch(existing -> existing.getId() != null && existing.getId().equals(record.getId()));
                if (!alreadyMatched) {
                    existingRecords.add(record);
                }
            }
        }

        // 3. Match exact device records by localId first. Singleton-style payloads also get a tenant-level fallback.
        Map<Integer, T> existingRecordMap = existingRecords.stream()
                .collect(Collectors.toMap(
                        BaseSyncEntity::getLocalId,
                        Function.identity(),
                        (existing, replacement) -> existing.getUpdatedAt() > replacement.getUpdatedAt() ? existing : replacement
                ));

        // Use a map for records to save to prevent multiple INSERT/UPDATE of same localId in one batch
        Map<Integer, T> recordsToSaveMap = new HashMap<>();

        // 4. Process the payload in memory
        for (T incomingRecord : payload) {
            try {
                if (incomingRecord.getLocalId() == null) {
                    if (singletonStylePayload) {
                        incomingRecord.setLocalId(1);
                    } else {
                        log.warn("Skipping record with NULL localId for device: {}", deviceId);
                        // We can't really report this back by ID if the ID is NULL, but we acknowledge it's skipped.
                        continue;
                    }
                }

                incomingRecord.setRestaurantId(tenantId);
                incomingRecord.setServerUpdatedAt(serverTime); // Enforce Server Time

                T existingRecord = existingRecordMap.get(incomingRecord.getLocalId());

                if (existingRecord != null) {
                    incomingRecord.setId(existingRecord.getId());

                    if (incomingRecord.getUpdatedAt() > existingRecord.getUpdatedAt()) {
                        T staged = recordsToSaveMap.get(incomingRecord.getLocalId());
                        if (staged == null || incomingRecord.getUpdatedAt() > staged.getUpdatedAt()) {
                            recordsToSaveMap.put(incomingRecord.getLocalId(), incomingRecord);
                        }
                        successfulLocalIds.add(incomingRecord.getLocalId());
                    } else {
                        // CRIT-06 fix: Acknowledge the record even if the server version is newer.
                        // This prevents the client from infinitely retrying a record that lost the LWW race.
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
                log.error("Sync Error for device {}: {}", incomingRecord.getDeviceId(), e.getMessage());
            }
        }

        // 5. Save all records to the DB in a single batch
        if (!recordsToSaveMap.isEmpty()) {
            repository.saveAll(new ArrayList<>(recordsToSaveMap.values()));
        }

        log.info("Successfully batch synced {} records for Tenant ID: {}", successfulLocalIds.size(), tenantId);
        
        return new PushSyncResponse(successfulLocalIds, failedLocalIds);
    }
}
