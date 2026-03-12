package com.khanabook.saas.service.impl;

import com.khanabook.saas.entity.MenuItem;
import com.khanabook.saas.entity.StockLog;
import com.khanabook.saas.repository.MenuItemRepository;
import com.khanabook.saas.repository.StockLogRepository;
import com.khanabook.saas.service.StockLogService;
import com.khanabook.saas.sync.service.GenericSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StockLogServiceImpl implements StockLogService {
    private final StockLogRepository repository;
    private final MenuItemRepository menuItemRepository;
    private final GenericSyncService genericSyncService;

    @Override
    public List<Integer> pushData(Long tenantId, List<StockLog> payload) {
        java.util.Iterator<StockLog> iterator = payload.iterator();
        while (iterator.hasNext()) {
            StockLog log = iterator.next();
            if (log.getServerMenuItemId() == null && log.getMenuItemId() != null) {
                Optional<MenuItem> menuItem = menuItemRepository.findByRestaurantIdAndDeviceIdAndLocalId(
                        tenantId, log.getDeviceId(), log.getMenuItemId());
                
                if (menuItem.isPresent()) {
                    log.setServerMenuItemId(menuItem.get().getId());
                } else {
                    // Skip this record to prevent corruption
                    System.err.println("WARNING: Skipping StockLog push. Could not resolve serverMenuItemId for localId: " 
                            + log.getMenuItemId() + " on device: " + log.getDeviceId());
                    iterator.remove();
                }
            }
        }
        return genericSyncService.handlePushSync(tenantId, payload, repository);
    }

    @Override
    public List<StockLog> pullData(Long tenantId, Long lastSyncTimestamp, String deviceId) {
        return repository.findByRestaurantIdAndUpdatedAtGreaterThanAndDeviceIdNot(tenantId, lastSyncTimestamp, deviceId);
    }
}
