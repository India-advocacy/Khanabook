package com.khanabook.saas.service;

import com.khanabook.saas.entity.Bill;
import com.khanabook.saas.repository.BillRepository;
import com.khanabook.saas.service.impl.BillServiceImpl;
import com.khanabook.saas.sync.service.GenericSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillServiceTest {

    @Mock
    private BillRepository billRepository;

    private GenericSyncService genericSyncService;
    private BillServiceImpl billService;

    @Captor
    private ArgumentCaptor<List<Bill>> listCaptor;

    private final Long AUTHENTICATED_RESTAURANT_ID = 99L;
    private final String DEVICE_ID = "TABLET_1";

    @BeforeEach
    void setUp() {
        genericSyncService = new GenericSyncService();
        billService = new BillServiceImpl(billRepository, genericSyncService);
    }

    private Bill createMobileBill(Integer localId, Long updatedAt) {
        Bill bill = new Bill();
        bill.setLocalId(localId);
        bill.setUpdatedAt(updatedAt);
        bill.setDeviceId(DEVICE_ID);
        return bill;
    }

    @Test
    void givenExistingBill_whenMobileIsNewer_thenUpdateLwwSuccess() {
        Long oldServerTime = 1000L;
        Long newMobileTime = 2000L;

        Bill existingDbBill = new Bill();
        existingDbBill.setId(5L);
        existingDbBill.setUpdatedAt(oldServerTime);
        existingDbBill.setDeviceId(DEVICE_ID);
        existingDbBill.setLocalId(101);

        Bill mobileBill = createMobileBill(101, newMobileTime);

        // Mock the BATCH lookup (findByRestaurantIdAndDeviceIdAndLocalIdIn)
        when(billRepository.findByRestaurantIdAndDeviceIdAndLocalIdIn(
                eq(AUTHENTICATED_RESTAURANT_ID), eq(DEVICE_ID), anyList()))
                .thenReturn(List.of(existingDbBill));

        // Mock saveAll to return the input list
        when(billRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        List<Integer> successIds = billService.pushData(AUTHENTICATED_RESTAURANT_ID, List.of(mobileBill));

        // Capture what was passed to saveAll
        verify(billRepository).saveAll(listCaptor.capture());
        Bill savedBill = listCaptor.getValue().get(0);

        assertThat(savedBill.getId()).isEqualTo(5L);
        assertThat(savedBill.getUpdatedAt()).isEqualTo(newMobileTime);
        assertThat(successIds).containsExactly(101);
    }

    @Test
    void givenHackedPayload_whenInsertNewBill_thenForceTenantIsolation() {
        Long maliciousRestaurantId = 666L;
        Bill hackedMobileBill = createMobileBill(202, 1000L);
        hackedMobileBill.setRestaurantId(maliciousRestaurantId);

        when(billRepository.findByRestaurantIdAndDeviceIdAndLocalIdIn(anyLong(), anyString(), anyList()))
                .thenReturn(List.of());

        when(billRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        billService.pushData(AUTHENTICATED_RESTAURANT_ID, List.of(hackedMobileBill));

        verify(billRepository).saveAll(listCaptor.capture());
        Bill savedBill = listCaptor.getValue().get(0);

        // Server MUST override the malicious ID with the authenticated one
        assertThat(savedBill.getRestaurantId()).isEqualTo(AUTHENTICATED_RESTAURANT_ID);
    }
}
