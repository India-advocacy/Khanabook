package com.khanabook.saas.service.impl;

import com.khanabook.saas.entity.MenuItem;
import com.khanabook.saas.entity.StockLog;
import com.khanabook.saas.entity.ItemVariant;
import com.khanabook.saas.repository.MenuItemRepository;
import com.khanabook.saas.repository.StockLogRepository;
import com.khanabook.saas.repository.ItemVariantRepository;
import com.khanabook.saas.service.StockLogService;
import com.khanabook.saas.sync.service.GenericSyncService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StockLogServiceImpl implements StockLogService {
    private static final Logger log_logger = LoggerFactory.getLogger(StockLogServiceImpl.class);
    private final StockLogRepository repository;
    private final MenuItemRepository menuItemRepository;
    private final ItemVariantRepository itemVariantRepository;
    private final GenericSyncService genericSyncService;

    @Override
    public List<Integer> pushData(Long tenantId, List<StockLog> payload) {
        java.util.Iterator<StockLog> iterator = payload.iterator();
        while (iterator.hasNext()) {
            StockLog log = iterator.next();
            
            // 1. Resolve MenuItem
            if (log.getServerMenuItemId() == null && log.getMenuItemId() != null) {
                Optional<MenuItem> menuItem = menuItemRepository.findByRestaurantIdAndDeviceIdAndLocalId(
                        tenantId, log.getDeviceId(), log.getMenuItemId());
                
                if (menuItem.isPresent()) {
                    log.setServerMenuItemId(menuItem.get().getId());
                } else {
                    // FALLBACK: Try resolving by Server ID directly.
                    Optional<MenuItem> serverMenuItem = menuItemRepository.findById(log.getMenuItemId().longValue());
                    if (serverMenuItem.isPresent() && serverMenuItem.get().getRestaurantId().equals(tenantId)) {
                        log.setServerMenuItemId(serverMenuItem.get().getId());
                    } else {
                        log_logger.warn("Skipping StockLog push. Could not resolve serverMenuItemId for localId: {} on device: {}", 
                                log.getMenuItemId(), log.getDeviceId());
                        iterator.remove();
                        continue;
                    }
                }
            }

            // 2. Resolve Variant (if applicable)
            if (log.getServerVariantId() == null && log.getVariantId() != null && log.getVariantId() > 0) {
                Optional<ItemVariant> variant = itemVariantRepository.findByRestaurantIdAndDeviceIdAndLocalId(
                        tenantId, log.getDeviceId(), log.getVariantId());
                
                if (variant.isPresent()) {
                    log.setServerVariantId(variant.get().getId());
                } else {
                    Optional<ItemVariant> serverVariant = itemVariantRepository.findById(log.getVariantId().longValue());
                    if (serverVariant.isPresent() && serverVariant.get().getRestaurantId().equals(tenantId)) {
                        log.setServerVariantId(serverVariant.get().getId());
                    }
                }
            }
        }
        return genericSyncService.handlePushSync(tenantId, payload, repository);
    }

    @Override
    public List<StockLog> pullData(Long tenantId, Long lastSyncTimestamp, String deviceId) {
        return repository.findByRestaurantIdAndServerUpdatedAtGreaterThanAndDeviceIdNot(tenantId, lastSyncTimestamp, deviceId);
    }
}
