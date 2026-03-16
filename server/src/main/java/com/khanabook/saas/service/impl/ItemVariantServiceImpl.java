package com.khanabook.saas.service.impl;

import com.khanabook.saas.entity.ItemVariant;
import com.khanabook.saas.repository.ItemVariantRepository;
import com.khanabook.saas.service.ItemVariantService;
import com.khanabook.saas.sync.dto.PushSyncResponse;
import com.khanabook.saas.sync.service.GenericSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemVariantServiceImpl implements ItemVariantService {
    private final ItemVariantRepository repository;
    private final GenericSyncService genericSyncService;

    @Override
    public PushSyncResponse pushData(Long tenantId, List<ItemVariant> payload) {
        return genericSyncService.handlePushSync(tenantId, payload, repository);
    }

    @Override
    public List<ItemVariant> pullData(Long tenantId, Long lastSyncTimestamp, String deviceId) {
        return repository.findByRestaurantIdAndServerUpdatedAtGreaterThanAndDeviceIdNot(tenantId, lastSyncTimestamp, deviceId);
    }
}
