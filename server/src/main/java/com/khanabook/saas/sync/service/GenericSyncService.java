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
	public <T extends BaseSyncEntity> PushSyncResponse handlePushSync(Long tenantId, List<T> payload,
			SyncRepository<T, Long> repository) {

		if (tenantId == null) {
			throw new IllegalArgumentException(
					"Tenant ID (Restaurant ID) cannot be null. Ensure valid JWT is provided.");
		}

		if (payload == null || payload.isEmpty()) {
			return new PushSyncResponse(new ArrayList<>(), new ArrayList<>());
		}

		List<Integer> successfulLocalIds = new ArrayList<>();
		List<Integer> failedLocalIds = new ArrayList<>();

		for (T record : payload) {
			if (record.getLocalId() == null && record.getId() != null) {
				record.setLocalId(record.getId().intValue());
				record.setId(null);
			}
		}

		Map<String, List<T>> recordsByDevice = payload.stream()
				.collect(Collectors.groupingBy(record -> record.getDeviceId() != null ? record.getDeviceId() : "unknown"));

		long serverTime = System.currentTimeMillis();
		List<T> allRecordsToSave = new ArrayList<>();

		for (Map.Entry<String, List<T>> entry : recordsByDevice.entrySet()) {
			String deviceId = entry.getKey();
			List<T> devicePayload = entry.getValue();

			List<Integer> incomingLocalIds = devicePayload.stream().map(BaseSyncEntity::getLocalId)
					.filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());

			BaseSyncEntity firstRecord = devicePayload.get(0);
			boolean isSingletonType = firstRecord instanceof RestaurantProfile || firstRecord instanceof User;
			boolean singletonStylePayload = isSingletonType && devicePayload.size() == 1
					&& (incomingLocalIds.isEmpty() || incomingLocalIds.contains(1));

			List<T> existingRecords = new ArrayList<>(
					repository.findByRestaurantIdAndDeviceIdAndLocalIdIn(tenantId, deviceId, incomingLocalIds));

			if (singletonStylePayload) {
				List<T> crossDeviceRecords = repository.findByRestaurantIdAndLocalIdIn(tenantId, List.of(1));

				for (T record : crossDeviceRecords) {
					boolean matchFound = false;
					if (record instanceof User existingUser && firstRecord instanceof User incomingUser) {
						if (existingUser.getEmail() != null
								&& existingUser.getEmail().equalsIgnoreCase(incomingUser.getEmail())) {
							matchFound = true;
						}
					} else {
						matchFound = true;
					}

					if (matchFound) {
						boolean alreadyMatched = existingRecords.stream()
								.anyMatch(existing -> existing.getId() != null && existing.getId().equals(record.getId()));
						if (!alreadyMatched) {
							existingRecords.add(record);
						}
					}
				}
			}

			Map<Integer, T> existingRecordMap = existingRecords.stream()
					.collect(Collectors.toMap(BaseSyncEntity::getLocalId, Function.identity(), (existing,
							replacement) -> existing.getUpdatedAt() > replacement.getUpdatedAt() ? existing : replacement));

			Map<Integer, T> recordsToSaveMap = new HashMap<>();

			for (T incomingRecord : devicePayload) {
				try {
					if (incomingRecord.getLocalId() == null) {
						if (singletonStylePayload) {
							incomingRecord.setLocalId(1);
						} else {
							log.warn("Skipping record with NULL localId for device: {}", deviceId);
							continue;
						}
					}

					incomingRecord.setRestaurantId(tenantId);
					incomingRecord.setServerUpdatedAt(serverTime);

					if (incomingRecord.getCreatedAt() == null) {
						incomingRecord.setCreatedAt(
								incomingRecord.getUpdatedAt() != null ? incomingRecord.getUpdatedAt() : serverTime);
					}

					T existingRecord = null;
					if (incomingRecord.getLocalId() != null) {
						if (incomingRecord.getId() != null) {
							existingRecord = existingRecords.stream().filter(r -> incomingRecord.getId().equals(r.getId()))
									.findFirst().orElse(null);
						}

						if (existingRecord == null) {
							existingRecord = existingRecordMap.get(incomingRecord.getLocalId());
						}
					}

					if (existingRecord != null) {
						if (incomingRecord.getUpdatedAt() > existingRecord.getUpdatedAt()) {

							if (incomingRecord instanceof User user && existingRecord instanceof User existingUser) {
								if (user.getPasswordHash() == null || user.getPasswordHash().isEmpty()) {
									user.setPasswordHash(existingUser.getPasswordHash());
								}
							}
							incomingRecord.setId(existingRecord.getId());

							T staged = recordsToSaveMap.get(incomingRecord.getLocalId());
							if (staged == null || incomingRecord.getUpdatedAt() > staged.getUpdatedAt()) {
								recordsToSaveMap.put(incomingRecord.getLocalId(), incomingRecord);
							}
							successfulLocalIds.add(incomingRecord.getLocalId());
						} else {
							successfulLocalIds.add(incomingRecord.getLocalId());
						}
					} else {

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
			
			allRecordsToSave.addAll(recordsToSaveMap.values());
		}

		if (!allRecordsToSave.isEmpty()) {
			repository.saveAll(allRecordsToSave);
		}

		log.info("Successfully batch synced {} records for Tenant ID: {}", successfulLocalIds.size(), tenantId);

		return new PushSyncResponse(successfulLocalIds, failedLocalIds);
	}
}
