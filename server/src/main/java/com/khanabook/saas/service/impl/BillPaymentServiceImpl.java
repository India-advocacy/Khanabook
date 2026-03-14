package com.khanabook.saas.service.impl;

import com.khanabook.saas.entity.Bill;
import com.khanabook.saas.entity.BillPayment;
import com.khanabook.saas.repository.BillRepository;
import com.khanabook.saas.repository.BillPaymentRepository;
import com.khanabook.saas.service.BillPaymentService;
import com.khanabook.saas.sync.service.GenericSyncService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BillPaymentServiceImpl implements BillPaymentService {
    private final BillPaymentRepository repository;
    private final BillRepository billRepository;
    private final GenericSyncService genericSyncService;

    @Override
    public List<Integer> pushData(Long tenantId, List<BillPayment> payload) {
        for (BillPayment payment : payload) {
            if (payment.getServerBillId() == null && payment.getBillId() != null) {
                Optional<Bill> bill = billRepository.findByRestaurantIdAndDeviceIdAndLocalId(
                        tenantId, payment.getDeviceId(), payment.getBillId());
                if (bill.isPresent()) {
                    payment.setServerBillId(bill.get().getId());
                } else {
                    billRepository.findById(payment.getBillId().longValue())
                        .filter(b -> b.getRestaurantId().equals(tenantId))
                        .ifPresent(b -> payment.setServerBillId(b.getId()));
                }
            }
        }
        return genericSyncService.handlePushSync(tenantId, payload, repository);
    }

    @Override
    public List<BillPayment> pullData(Long tenantId, Long lastSyncTimestamp, String deviceId) {
        return repository.findByRestaurantIdAndServerUpdatedAtGreaterThanAndDeviceIdNot(tenantId, lastSyncTimestamp, deviceId);
    }
}
