package com.khanabook.saas.service.impl;

import com.khanabook.saas.entity.Bill;
import com.khanabook.saas.repository.BillRepository;
import com.khanabook.saas.service.BillService;
import com.khanabook.saas.sync.dto.PushSyncResponse;
import com.khanabook.saas.sync.service.GenericSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Date;
import java.text.SimpleDateFormat;

@Service
@RequiredArgsConstructor
public class BillServiceImpl implements BillService {
    private final BillRepository repository;
    private final GenericSyncService genericSyncService;

    @Override
    public PushSyncResponse pushData(Long tenantId, List<Bill> payload) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        for (Bill bill : payload) {
            if (bill.getLastResetDate() == null) {
                // Derive lastResetDate from createdAt if missing to satisfy uniqueness constraint
                Long created = bill.getCreatedAt() != null ? bill.getCreatedAt() : System.currentTimeMillis();
                bill.setLastResetDate(sdf.format(new Date(created)));
            }
        }
        return genericSyncService.handlePushSync(tenantId, payload, repository);
    }

    @Override
    public List<Bill> pullData(Long tenantId, Long lastSyncTimestamp, String deviceId) {
        return repository.findByRestaurantIdAndServerUpdatedAtGreaterThanAndDeviceIdNot(tenantId, lastSyncTimestamp, deviceId);
    }
}
