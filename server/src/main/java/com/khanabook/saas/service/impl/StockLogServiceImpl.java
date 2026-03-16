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
import java.util.Iterator;

@Service
@RequiredArgsConstructor
public class StockLogServiceImpl implements StockLogService {
    private static final Logger log = LoggerFactory.getLogger(StockLogServiceImpl.class);
    private final StockLogRepository repository;
    private final MenuItemRepository menuItemRepository;
    private final ItemVariantRepository itemVariantRepository;
    private final GenericSyncService genericSyncService;

    /**
     * Pushes stock logs and recalculates current stock levels.
     * 
     * NOTE: Stock logs are the canonical source of truth for stock levels.
     * currentStock on MenuItem/ItemVariant is a denormalized value recalculated 
     * from logs to ensure multi-device consistency without LWW race conditions.
     */
    @Override
    @Transactional
    public PushSyncResponse pushData(Long tenantId, List<StockLog> payload) {
        List<StockLog> toSync = new ArrayList<>(payload);
        List<Integer> failedLocalIds = new ArrayList<>();
        Iterator<StockLog> iterator = toSync.iterator();

        while (iterator.hasNext()) {
            StockLog stockLog = iterator.next();
            
            // 1. Resolve MenuItem
            if (stockLog.getServerMenuItemId() == null && stockLog.getMenuItemId() != null) {
                Optional<MenuItem> menuItem = menuItemRepository.findByRestaurantIdAndDeviceIdAndLocalId(
                        tenantId, stockLog.getDeviceId(), stockLog.getMenuItemId());
                
                if (menuItem.isPresent()) {
                    stockLog.setServerMenuItemId(menuItem.get().getId());
                } else {
                    Optional<MenuItem> serverMenuItem = menuItemRepository.findById(stockLog.getMenuItemId().longValue());
                    if (serverMenuItem.isPresent() && serverMenuItem.get().getRestaurantId().equals(tenantId)) {
                        stockLog.setServerMenuItemId(serverMenuItem.get().getId());
                    } else {
                        log.warn("Skipping StockLog push. Could not resolve serverMenuItemId for localId: {} on device: {}", 
                                stockLog.getMenuItemId(), stockLog.getDeviceId());
                        failedLocalIds.add(stockLog.getLocalId());
                        iterator.remove();
                        continue;
                    }
                }
            }

            // 2. Resolve Variant (if applicable)
            if (stockLog.getServerVariantId() == null && stockLog.getVariantId() != null && stockLog.getVariantId() > 0) {
                Optional<ItemVariant> variant = itemVariantRepository.findByRestaurantIdAndDeviceIdAndLocalId(
                        tenantId, stockLog.getDeviceId(), stockLog.getVariantId());
                
                if (variant.isPresent()) {
                    stockLog.setServerVariantId(variant.get().getId());
                } else {
                    Optional<ItemVariant> serverVariant = itemVariantRepository.findById(stockLog.getVariantId().longValue());
                    if (serverVariant.isPresent() && serverVariant.get().getRestaurantId().equals(tenantId)) {
                        stockLog.setServerVariantId(serverVariant.get().getId());
                    }
                }
            }
        }
        
        PushSyncResponse response = genericSyncService.handlePushSync(tenantId, toSync, repository);
        
        // Post-Sync: Recalculate Stock levels for affected items/variants
        Set<Long> affectedMenuItems = new HashSet<>();
        Set<Long> affectedVariants = new HashSet<>();
        
        for (StockLog stockLog : toSync) {
            if (response.getSuccessfulLocalIds().contains(stockLog.getLocalId())) {
                if (stockLog.getServerMenuItemId() != null) affectedMenuItems.add(stockLog.getServerMenuItemId());
                if (stockLog.getServerVariantId() != null) affectedVariants.add(stockLog.getServerVariantId());
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
