package com.khanabook.saas.service.impl;

import com.khanabook.saas.entity.RestaurantProfile;
import com.khanabook.saas.repository.RestaurantProfileRepository;
import com.khanabook.saas.service.RestaurantProfileService;
import com.khanabook.saas.sync.service.GenericSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RestaurantProfileServiceImpl implements RestaurantProfileService {
    private final RestaurantProfileRepository repository;
    private final GenericSyncService genericSyncService;

    @Override
    public List<Integer> pushData(Long tenantId, List<RestaurantProfile> payload) {
        return genericSyncService.handlePushSync(tenantId, payload, repository);
    }

    @Override
    public List<RestaurantProfile> pullData(Long tenantId, Long lastSyncTimestamp, String deviceId) {
        return repository.findByRestaurantIdAndServerUpdatedAtGreaterThanAndDeviceIdNot(tenantId, lastSyncTimestamp, deviceId);
    }

    @Override
    @Transactional
    public CounterResponse incrementAndGetCounters(Long tenantId, String today) {
        Long now = System.currentTimeMillis();
        int updated = repository.incrementCountersAtomic(tenantId, today, now);
        
        if (updated == 0) {
            throw new RuntimeException("Restaurant profile not found for ID: " + tenantId);
        }

        java.util.List<Object[]> result = repository.getCounters(tenantId);
        if (result == null || result.isEmpty()) {
            throw new RuntimeException("Failed to retrieve updated counters");
        }

        CounterResponse response = new CounterResponse();
        Object[] row = result.get(0);
        response.setDailyCounter(((Number) row[0]).intValue());
        response.setLifetimeCounter(((Number) row[1]).intValue());
        return response;
    }
}
