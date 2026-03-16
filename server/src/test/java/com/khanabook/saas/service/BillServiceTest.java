package com.khanabook.saas.service;

import com.khanabook.saas.entity.Bill;
import com.khanabook.saas.repository.BillRepository;
import com.khanabook.saas.service.impl.BillServiceImpl;
import com.khanabook.saas.sync.dto.PushSyncResponse;
import com.khanabook.saas.sync.service.GenericSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BillServiceTest {

    @Mock
    private BillRepository billRepository;

    private GenericSyncService genericSyncService;
    private BillServiceImpl billService;

    @Captor
    private ArgumentCaptor<Iterable<Bill>> listCaptor;

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
        doAnswer(i -> i.getArgument(0)).when(billRepository).saveAll(anyList());

        PushSyncResponse response = billService.pushData(AUTHENTICATED_RESTAURANT_ID, List.of(mobileBill));

        // Capture what was passed to saveAll
        verify(billRepository).saveAll(listCaptor.capture());
        Bill savedBill = listCaptor.getValue().iterator().next();

        assertThat(savedBill.getId()).isEqualTo(5L);
        assertThat(savedBill.getUpdatedAt()).isEqualTo(newMobileTime);
        assertThat(response.getSuccessfulLocalIds()).containsExactly(101);
    }

    @Test
    void givenHackedPayload_whenInsertNewBill_thenForceTenantIsolation() {
        Long maliciousRestaurantId = 666L;
        Bill hackedMobileBill = createMobileBill(202, 1000L);
        hackedMobileBill.setRestaurantId(maliciousRestaurantId);

        when(billRepository.findByRestaurantIdAndDeviceIdAndLocalIdIn(anyLong(), anyString(), anyList()))
                .thenReturn(List.of());

        doAnswer(i -> i.getArgument(0)).when(billRepository).saveAll(anyList());

        billService.pushData(AUTHENTICATED_RESTAURANT_ID, List.of(hackedMobileBill));

        verify(billRepository).saveAll(listCaptor.capture());
        Bill savedBill = listCaptor.getValue().iterator().next();

        // Server MUST override the malicious ID with the authenticated one
        assertThat(savedBill.getRestaurantId()).isEqualTo(AUTHENTICATED_RESTAURANT_ID);
    }
}
