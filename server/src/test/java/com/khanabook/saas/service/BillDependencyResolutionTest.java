package com.khanabook.saas.service;

import com.khanabook.saas.entity.*;
import com.khanabook.saas.repository.*;
import com.khanabook.saas.service.impl.BillItemServiceImpl;
import com.khanabook.saas.service.impl.BillPaymentServiceImpl;
import com.khanabook.saas.sync.dto.PushSyncResponse;
import com.khanabook.saas.sync.service.GenericSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BillDependencyResolutionTest {

    @Mock private BillItemRepository billItemRepo;
    @Mock private BillPaymentRepository billPaymentRepo;
    @Mock private BillRepository billRepo;
    @Mock private MenuItemRepository menuItemRepo;
    @Mock private ItemVariantRepository itemVariantRepo;

    private BillItemServiceImpl billItemService;
    private BillPaymentServiceImpl billPaymentService;

    private static final Long TENANT_ID = 55L;
    private static final String DEVICE = "PHONE_1";

    @BeforeEach
    void setUp() {
        GenericSyncService gs = new GenericSyncService();
        billItemService = new BillItemServiceImpl(billItemRepo, billRepo, menuItemRepo, itemVariantRepo, gs);
        billPaymentService = new BillPaymentServiceImpl(billPaymentRepo, billRepo, gs);
    }

    // ─── BillItem: bill resolution ────────────────────────────────────────────

    @Test
    void billItem_resolvesBillByDeviceAndLocalId() {
        Bill bill = serverBill(200L);
        MenuItem mi = serverMenuItem(300L);
        BillItem item = billItem(1, 10, 20, null);

        when(billRepo.findByRestaurantIdAndDeviceIdAndLocalId(TENANT_ID, DEVICE, 10))
            .thenReturn(Optional.of(bill));
        when(menuItemRepo.findByRestaurantIdAndDeviceIdAndLocalId(TENANT_ID, DEVICE, 20))
            .thenReturn(Optional.of(mi));
        stubBillItemSync();

        billItemService.pushData(TENANT_ID, List.of(item));

        assertThat(item.getServerBillId()).isEqualTo(200L);
        assertThat(item.getServerMenuItemId()).isEqualTo(300L);
    }

    @Test
    void billItem_missingBill_addedToFailedIds() {
        BillItem item = billItem(1, 10, 20, null);
        item.setLocalId(77);

        when(billRepo.findByRestaurantIdAndDeviceIdAndLocalId(any(), any(), anyInt()))
            .thenReturn(Optional.empty());
        when(billRepo.findById(anyLong())).thenReturn(Optional.empty());
        // MenuItem lookup doesn't matter — bill is the blocker

        PushSyncResponse resp = billItemService.pushData(TENANT_ID, List.of(item));

        assertThat(resp.getFailedLocalIds()).contains(77);
        assertThat(resp.getSuccessfulLocalIds()).doesNotContain(77);
    }

    @Test
    void billItem_missingMenuItem_addedToFailedIds() {
        Bill bill = serverBill(200L);
        BillItem item = billItem(1, 10, 20, null);
        item.setLocalId(88);

        when(billRepo.findByRestaurantIdAndDeviceIdAndLocalId(TENANT_ID, DEVICE, 10))
            .thenReturn(Optional.of(bill));
        when(menuItemRepo.findByRestaurantIdAndDeviceIdAndLocalId(any(), any(), anyInt()))
            .thenReturn(Optional.empty());
        when(menuItemRepo.findById(anyLong())).thenReturn(Optional.empty());

        PushSyncResponse resp = billItemService.pushData(TENANT_ID, List.of(item));

        assertThat(resp.getFailedLocalIds()).contains(88);
    }

    @Test
    void billItem_variantResolved_serverVariantIdSet() {
        Bill bill = serverBill(200L);
        MenuItem mi = serverMenuItem(300L);
        ItemVariant iv = serverVariant(400L);
        BillItem item = billItem(1, 10, 20, 30);

        when(billRepo.findByRestaurantIdAndDeviceIdAndLocalId(TENANT_ID, DEVICE, 10))
            .thenReturn(Optional.of(bill));
        when(menuItemRepo.findByRestaurantIdAndDeviceIdAndLocalId(TENANT_ID, DEVICE, 20))
            .thenReturn(Optional.of(mi));
        when(itemVariantRepo.findByRestaurantIdAndDeviceIdAndLocalId(TENANT_ID, DEVICE, 30))
            .thenReturn(Optional.of(iv));
        stubBillItemSync();

        billItemService.pushData(TENANT_ID, List.of(item));

        assertThat(item.getServerVariantId()).isEqualTo(400L);
    }

    @Test
    void billItem_zeroVariantId_variantResolutionSkipped() {
        Bill bill = serverBill(200L);
        MenuItem mi = serverMenuItem(300L);
        BillItem item = billItem(1, 10, 20, 0); // variantId = 0 = no variant

        when(billRepo.findByRestaurantIdAndDeviceIdAndLocalId(TENANT_ID, DEVICE, 10))
            .thenReturn(Optional.of(bill));
        when(menuItemRepo.findByRestaurantIdAndDeviceIdAndLocalId(TENANT_ID, DEVICE, 20))
            .thenReturn(Optional.of(mi));
        stubBillItemSync();

        billItemService.pushData(TENANT_ID, List.of(item));

        verifyNoInteractions(itemVariantRepo);
    }

    @Test
    void billItem_serverBillIdAlreadySet_skipsBillLookup() {
        MenuItem mi = serverMenuItem(300L);
        BillItem item = billItem(1, 10, 20, null);
        item.setServerBillId(200L); // already resolved by client

        when(menuItemRepo.findByRestaurantIdAndDeviceIdAndLocalId(TENANT_ID, DEVICE, 20))
            .thenReturn(Optional.of(mi));
        stubBillItemSync();

        billItemService.pushData(TENANT_ID, List.of(item));

        verify(billRepo, never()).findByRestaurantIdAndDeviceIdAndLocalId(any(), any(), anyInt());
    }

    // ─── BillPayment: bill resolution ─────────────────────────────────────────

    @Test
    void billPayment_resolvesBillByDeviceAndLocalId() {
        Bill bill = serverBill(200L);
        BillPayment payment = billPayment(1, 10);

        when(billRepo.findByRestaurantIdAndDeviceIdAndLocalId(TENANT_ID, DEVICE, 10))
            .thenReturn(Optional.of(bill));
        stubBillPaymentSync();

        billPaymentService.pushData(TENANT_ID, List.of(payment));

        assertThat(payment.getServerBillId()).isEqualTo(200L);
    }

    @Test
    void billPayment_billNotFound_addedToFailedIds() {
        BillPayment payment = billPayment(1, 10);
        payment.setLocalId(55);

        when(billRepo.findByRestaurantIdAndDeviceIdAndLocalId(any(), any(), anyInt()))
            .thenReturn(Optional.empty());
        when(billRepo.findById(anyLong())).thenReturn(Optional.empty());

        PushSyncResponse resp = billPaymentService.pushData(TENANT_ID, List.of(payment));

        assertThat(resp.getFailedLocalIds()).contains(55);
        assertThat(resp.getSuccessfulLocalIds()).doesNotContain(55);
    }

    @Test
    void billPayment_wrongTenantBill_addedToFailedIds() {
        Bill wrongTenantBill = serverBill(200L);
        wrongTenantBill.setRestaurantId(999L); // different tenant
        BillPayment payment = billPayment(1, 10);
        payment.setLocalId(66);

        when(billRepo.findByRestaurantIdAndDeviceIdAndLocalId(any(), any(), anyInt()))
            .thenReturn(Optional.empty());
        when(billRepo.findById(10L)).thenReturn(Optional.of(wrongTenantBill));

        PushSyncResponse resp = billPaymentService.pushData(TENANT_ID, List.of(payment));

        assertThat(resp.getFailedLocalIds()).contains(66);
    }

    @Test
    void billPayment_serverBillIdAlreadySet_skipsBillLookup() {
        BillPayment payment = billPayment(1, 10);
        payment.setServerBillId(200L); // already resolved
        stubBillPaymentSync();

        billPaymentService.pushData(TENANT_ID, List.of(payment));

        verify(billRepo, never()).findByRestaurantIdAndDeviceIdAndLocalId(any(), any(), anyInt());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private BillItem billItem(int localId, int billId, int menuItemId, Integer variantId) {
        BillItem bi = new BillItem();
        bi.setLocalId(localId);
        bi.setDeviceId(DEVICE);
        bi.setRestaurantId(TENANT_ID);
        bi.setUpdatedAt(1000L);
        bi.setBillId(billId);
        bi.setMenuItemId(menuItemId);
        bi.setVariantId(variantId);
        bi.setItemName("Chai");
        bi.setQuantity(1);
        bi.setPrice(BigDecimal.TEN);
        bi.setItemTotal(BigDecimal.TEN);
        return bi;
    }

    private BillPayment billPayment(int localId, int billId) {
        BillPayment bp = new BillPayment();
        bp.setLocalId(localId);
        bp.setDeviceId(DEVICE);
        bp.setRestaurantId(TENANT_ID);
        bp.setUpdatedAt(1000L);
        bp.setBillId(billId);
        bp.setPaymentMode("CASH");
        bp.setAmount(BigDecimal.TEN);
        return bp;
    }

    private Bill serverBill(Long serverId) {
        Bill b = new Bill();
        b.setId(serverId);
        b.setRestaurantId(TENANT_ID);
        return b;
    }

    private MenuItem serverMenuItem(Long serverId) {
        MenuItem mi = new MenuItem();
        mi.setId(serverId);
        mi.setRestaurantId(TENANT_ID);
        return mi;
    }

    private ItemVariant serverVariant(Long serverId) {
        ItemVariant iv = new ItemVariant();
        iv.setId(serverId);
        iv.setRestaurantId(TENANT_ID);
        return iv;
    }

    private void stubBillItemSync() {
        when(billItemRepo.findByRestaurantIdAndDeviceIdAndLocalIdIn(any(), any(), anyList()))
            .thenReturn(new java.util.ArrayList<>());
        doAnswer(i -> i.getArgument(0)).when(billItemRepo).saveAll(any());
    }

    private void stubBillPaymentSync() {
        when(billPaymentRepo.findByRestaurantIdAndDeviceIdAndLocalIdIn(any(), any(), anyList()))
            .thenReturn(new java.util.ArrayList<>());
        doAnswer(i -> i.getArgument(0)).when(billPaymentRepo).saveAll(any());
    }
}
