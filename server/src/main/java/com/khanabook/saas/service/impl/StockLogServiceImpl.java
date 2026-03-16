package com.khanabook.saas.service.impl;

import com.khanabook.saas.entity.MenuItem;
import com.khanabook.saas.entity.StockLog;
import com.khanabook.saas.entity.ItemVariant;
import com.khanabook.saas.repository.MenuItemRepository;
import com.khanabook.saas.repository.StockLogRepository;
import com.khanabook.saas.repository.ItemVariantRepository;
import com.khanabook.saas.service.StockLogService;
import com.khanabook.saas.sync.dto.PushSyncResponse;
import com.khanabook.saas.sync.service.GenericSyncService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

@Service
@RequiredArgsConstructor
public class StockLogServiceImpl implements StockLogService {
    private static final Logger log_logger = LoggerFactory.getLogger(StockLogServiceImpl.class);
    private final StockLogRepository repository;
    private final MenuItemRepository menuItemRepository;
    private final ItemVariantRepository itemVariantRepository;
    private final GenericSyncService genericSyncService;

    @Override
    @Transactional
    public PushSyncResponse pushData(Long tenantId, List<StockLog> payload) {
        List<StockLog> toSync = new ArrayList<>(payload);
        List<Integer> failedLocalIds = new ArrayList<>();
        java.util.Iterator<StockLog> iterator = toSync.iterator();

        while (iterator.hasNext()) {
            StockLog log = iterator.next();
            
            // 1. Resolve MenuItem
            if (log.getServerMenuItemId() == null && log.getMenuItemId() != null) {
                Optional<MenuItem> menuItem = menuItemRepository.findByRestaurantIdAndDeviceIdAndLocalId(
                        tenantId, log.getDeviceId(), log.getMenuItemId());
                
                if (menuItem.isPresent()) {
                    log.setServerMenuItemId(menuItem.get().getId());
                } else {
                    Optional<MenuItem> serverMenuItem = menuItemRepository.findById(log.getMenuItemId().longValue());
                    if (serverMenuItem.isPresent() && serverMenuItem.get().getRestaurantId().equals(tenantId)) {
                        log.setServerMenuItemId(serverMenuItem.get().getId());
                    } else {
                        log_logger.warn("Skipping StockLog push. Could not resolve serverMenuItemId for localId: {} on device: {}", 
                                log.getMenuItemId(), log.getDeviceId());
                        failedLocalIds.add(log.getLocalId());
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
        
        PushSyncResponse response = genericSyncService.handlePushSync(tenantId, toSync, repository);
        
        // Post-Sync: Recalculate Stock levels for affected items/variants
        Set<Long> affectedMenuItems = new HashSet<>();
        Set<Long> affectedVariants = new HashSet<>();
        
        for (StockLog log : toSync) {
            if (response.getSuccessfulLocalIds().contains(log.getLocalId())) {
                if (log.getServerMenuItemId() != null) affectedMenuItems.add(log.getServerMenuItemId());
                if (log.getServerVariantId() != null) affectedVariants.add(log.getServerVariantId());
            }
        }
        
        for (Long menuId : affectedMenuItems) {
            menuItemRepository.recalculateStock(menuId);
        }
        for (Long varId : affectedVariants) {
            itemVariantRepository.recalculateStock(varId);
        }

        response.getFailedLocalIds().addAll(failedLocalIds);
        return response;
    }

    @Override
    public List<StockLog> pullData(Long tenantId, Long lastSyncTimestamp, String deviceId) {
        return repository.findByRestaurantIdAndServerUpdatedAtGreaterThanAndDeviceIdNot(tenantId, lastSyncTimestamp, deviceId);
    }
}
