package com.khanabook.saas.service.impl;

import com.khanabook.saas.entity.Bill;
import com.khanabook.saas.entity.MenuItem;
import com.khanabook.saas.entity.ItemVariant;
import com.khanabook.saas.entity.BillItem;
import com.khanabook.saas.repository.BillRepository;
import com.khanabook.saas.repository.MenuItemRepository;
import com.khanabook.saas.repository.ItemVariantRepository;
import com.khanabook.saas.repository.BillItemRepository;
import com.khanabook.saas.service.BillItemService;
import com.khanabook.saas.sync.service.GenericSyncService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BillItemServiceImpl implements BillItemService {
    private final BillItemRepository repository;
    private final BillRepository billRepository;
    private final MenuItemRepository menuItemRepository;
    private final ItemVariantRepository itemVariantRepository;
    private final GenericSyncService genericSyncService;

    @Override
    public List<Integer> pushData(Long tenantId, List<BillItem> payload) {
        for (BillItem item : payload) {
            // 1. Resolve Bill
            if (item.getServerBillId() == null && item.getBillId() != null) {
                Optional<Bill> bill = billRepository.findByRestaurantIdAndDeviceIdAndLocalId(
                        tenantId, item.getDeviceId(), item.getBillId());
                if (bill.isPresent()) {
                    item.setServerBillId(bill.get().getId());
                } else {
                    // Fallback to Server ID
                    billRepository.findById(item.getBillId().longValue())
                        .filter(b -> b.getRestaurantId().equals(tenantId))
                        .ifPresent(b -> item.setServerBillId(b.getId()));
                }
            }

            // 2. Resolve MenuItem
            if (item.getServerMenuItemId() == null && item.getMenuItemId() != null) {
                Optional<MenuItem> mi = menuItemRepository.findByRestaurantIdAndDeviceIdAndLocalId(
                        tenantId, item.getDeviceId(), item.getMenuItemId());
                if (mi.isPresent()) {
                    item.setServerMenuItemId(mi.get().getId());
                } else {
                    menuItemRepository.findById(item.getMenuItemId().longValue())
                        .filter(m -> m.getRestaurantId().equals(tenantId))
                        .ifPresent(m -> item.setServerMenuItemId(m.getId()));
                }
            }

            // 3. Resolve Variant
            if (item.getServerVariantId() == null && item.getVariantId() != null && item.getVariantId() > 0) {
                Optional<ItemVariant> iv = itemVariantRepository.findByRestaurantIdAndDeviceIdAndLocalId(
                        tenantId, item.getDeviceId(), item.getVariantId());
                if (iv.isPresent()) {
                    item.setServerVariantId(iv.get().getId());
                } else {
                    itemVariantRepository.findById(item.getVariantId().longValue())
                        .filter(v -> v.getRestaurantId().equals(tenantId))
                        .ifPresent(v -> item.setServerVariantId(v.getId()));
                }
            }
        }
        return genericSyncService.handlePushSync(tenantId, payload, repository);
    }

    @Override
    public List<BillItem> pullData(Long tenantId, Long lastSyncTimestamp, String deviceId) {
        return repository.findByRestaurantIdAndUpdatedAtGreaterThanAndDeviceIdNot(tenantId, lastSyncTimestamp, deviceId);
    }
}
