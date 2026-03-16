package com.khanabook.saas.service.impl;

import com.khanabook.saas.entity.Category;
import com.khanabook.saas.entity.MenuItem;
import com.khanabook.saas.repository.CategoryRepository;
import com.khanabook.saas.repository.MenuItemRepository;
import com.khanabook.saas.service.MenuItemService;
import com.khanabook.saas.sync.dto.PushSyncResponse;
import com.khanabook.saas.sync.service.GenericSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class MenuItemServiceImpl implements MenuItemService {
    private final MenuItemRepository repository;
    private final CategoryRepository categoryRepository;
    private final GenericSyncService genericSyncService;

    @Override
    public PushSyncResponse pushData(Long tenantId, List<MenuItem> payload) {
        List<MenuItem> toSync = new ArrayList<>();
        List<Integer> failedLocalIds = new ArrayList<>();

        for (MenuItem item : payload) {
            if (item.getServerCategoryId() == null && item.getCategoryId() != null) {
                Optional<Category> category = categoryRepository.findByRestaurantIdAndDeviceIdAndLocalId(
                        tenantId, item.getDeviceId(), item.getCategoryId());
                
                if (category.isPresent()) {
                    item.setServerCategoryId(category.get().getId());
                } else {
                    // Fallback to direct ID resolution if it's already a server ID sent by client incorrectly
                    Optional<Category> serverCategory = categoryRepository.findById(item.getCategoryId().longValue());
                    if (serverCategory.isPresent() && serverCategory.get().getRestaurantId().equals(tenantId)) {
                        item.setServerCategoryId(serverCategory.get().getId());
                    } else {
                        // Critical dependency missing
                        failedLocalIds.add(item.getLocalId());
                        continue;
                    }
                }
            }
            toSync.add(item);
        }

        PushSyncResponse response = genericSyncService.handlePushSync(tenantId, toSync, repository);
        response.getFailedLocalIds().addAll(failedLocalIds);
        return response;
    }

    @Override
    public List<MenuItem> pullData(Long tenantId, Long lastSyncTimestamp, String deviceId) {
        return repository.findByRestaurantIdAndServerUpdatedAtGreaterThanAndDeviceIdNot(tenantId, lastSyncTimestamp, deviceId);
    }
}
