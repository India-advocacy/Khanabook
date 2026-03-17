package com.khanabook.saas.service;

import com.khanabook.saas.entity.ItemVariant;
import com.khanabook.saas.entity.MenuItem;
import com.khanabook.saas.entity.StockLog;
import com.khanabook.saas.repository.ItemVariantRepository;
import com.khanabook.saas.repository.MenuItemRepository;
import com.khanabook.saas.repository.StockLogRepository;
import com.khanabook.saas.service.impl.StockLogServiceImpl;
import com.khanabook.saas.sync.dto.PushSyncResponse;
import com.khanabook.saas.sync.service.GenericSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StockLogServiceImplTest {

    @Mock private StockLogRepository stockLogRepository;
    @Mock private MenuItemRepository menuItemRepository;
    @Mock private ItemVariantRepository itemVariantRepository;

    private GenericSyncService genericSyncService;
    private StockLogServiceImpl service;

    private static final Long TENANT_ID = 10L;
    private static final String DEVICE = "TAB_1";

    @BeforeEach
    void setUp() {
        genericSyncService = new GenericSyncService();
        service = new StockLogServiceImpl(
            stockLogRepository, menuItemRepository, itemVariantRepository, genericSyncService
        );
    }

    

    @Test
    void push_resolvesMenuItemByDeviceAndLocalId() {
        MenuItem mi = menuItem(100L);
        StockLog sl = stockLog(1, 1000L, 5, null);

        when(menuItemRepository.findByRestaurantIdAndDeviceIdAndLocalId(TENANT_ID, DEVICE, 5))
            .thenReturn(Optional.of(mi));
        stubSyncService();

        service.pushData(TENANT_ID, List.of(sl));

        assertThat(sl.getServerMenuItemId()).isEqualTo(100L);
    }

    @Test
    void push_fallsBackToServerIdWhenLocalLookupFails() {
        MenuItem mi = menuItem(200L);
        mi.setRestaurantId(TENANT_ID);
        StockLog sl = stockLog(1, 1000L, 5, null);

        when(menuItemRepository.findByRestaurantIdAndDeviceIdAndLocalId(any(), any(), anyInt()))
            .thenReturn(Optional.empty());
        when(menuItemRepository.findById(5L)).thenReturn(Optional.of(mi));
        stubSyncService();

        service.pushData(TENANT_ID, List.of(sl));

        assertThat(sl.getServerMenuItemId()).isEqualTo(200L);
    }

    @Test
    void push_menuItemNotFound_addedToFailedIds() {
        StockLog sl = stockLog(1, 1000L, 5, null);
        sl.setLocalId(99);

        when(menuItemRepository.findByRestaurantIdAndDeviceIdAndLocalId(any(), any(), anyInt()))
            .thenReturn(Optional.empty());
        when(menuItemRepository.findById(anyLong())).thenReturn(Optional.empty());

        PushSyncResponse resp = service.pushData(TENANT_ID, List.of(sl));

        assertThat(resp.getFailedLocalIds()).contains(99);
        assertThat(resp.getSuccessfulLocalIds()).doesNotContain(99);
    }

    @Test
    void push_menuItemWrongTenant_addedToFailedIds() {
        MenuItem mi = menuItem(200L);
        mi.setRestaurantId(999L); 
        StockLog sl = stockLog(1, 1000L, 5, null);
        sl.setLocalId(88);

        when(menuItemRepository.findByRestaurantIdAndDeviceIdAndLocalId(any(), any(), anyInt()))
            .thenReturn(Optional.empty());
        when(menuItemRepository.findById(5L)).thenReturn(Optional.of(mi));

        PushSyncResponse resp = service.pushData(TENANT_ID, List.of(sl));

        assertThat(resp.getFailedLocalIds()).contains(88);
    }

    

    @Test
    void push_resolvesVariantWhenPresent() {
        MenuItem mi = menuItem(100L);
        ItemVariant iv = itemVariant(50L, TENANT_ID);
        StockLog sl = stockLog(1, 1000L, 5, 10);
        sl.setServerMenuItemId(100L); 

        when(menuItemRepository.findByRestaurantIdAndDeviceIdAndLocalId(any(), any(), anyInt()))
            .thenReturn(Optional.of(mi));
        when(itemVariantRepository.findByRestaurantIdAndDeviceIdAndLocalId(TENANT_ID, DEVICE, 10))
            .thenReturn(Optional.of(iv));
        stubSyncService();

        service.pushData(TENANT_ID, List.of(sl));

        assertThat(sl.getServerVariantId()).isEqualTo(50L);
    }

    @Test
    void push_variantIdZero_variantResolutionSkipped() {
        MenuItem mi = menuItem(100L);
        StockLog sl = stockLog(1, 1000L, 5, 0); 
        sl.setServerMenuItemId(100L);

        when(menuItemRepository.findByRestaurantIdAndDeviceIdAndLocalId(any(), any(), anyInt()))
            .thenReturn(Optional.of(mi));
        stubSyncService();

        service.pushData(TENANT_ID, List.of(sl));

        verifyNoInteractions(itemVariantRepository);
        assertThat(sl.getServerVariantId()).isNull();
    }

    

    @Test
    void push_successfulSync_triggersRecalculateForAffectedItems() {
        MenuItem mi = menuItem(100L);
        StockLog sl = stockLog(1, 1000L, 5, null);
        sl.setLocalId(1);

        when(menuItemRepository.findByRestaurantIdAndDeviceIdAndLocalId(any(), any(), anyInt()))
            .thenReturn(Optional.of(mi));

        
        when(stockLogRepository.findByRestaurantIdAndDeviceIdAndLocalIdIn(any(), any(), anyList()))
            .thenReturn(List.of());
        doAnswer(i -> i.getArgument(0)).when(stockLogRepository).saveAll(any());

        service.pushData(TENANT_ID, List.of(sl));

        verify(menuItemRepository).recalculateStock(100L);
    }

    @Test
    void push_failedResolution_doesNotTriggerRecalculate() {
        StockLog sl = stockLog(1, 1000L, 5, null);

        when(menuItemRepository.findByRestaurantIdAndDeviceIdAndLocalId(any(), any(), anyInt()))
            .thenReturn(Optional.empty());
        when(menuItemRepository.findById(anyLong())).thenReturn(Optional.empty());

        service.pushData(TENANT_ID, List.of(sl));

        verify(menuItemRepository, never()).recalculateStock(anyLong());
    }

    @Test
    void push_variantAffected_recalculatesVariantStock() {
        MenuItem mi = menuItem(100L);
        ItemVariant iv = itemVariant(50L, TENANT_ID);
        StockLog sl = stockLog(1, 1000L, 5, 10);
        sl.setLocalId(1);

        when(menuItemRepository.findByRestaurantIdAndDeviceIdAndLocalId(any(), any(), anyInt()))
            .thenReturn(Optional.of(mi));
        when(itemVariantRepository.findByRestaurantIdAndDeviceIdAndLocalId(any(), any(), eq(10)))
            .thenReturn(Optional.of(iv));
        when(stockLogRepository.findByRestaurantIdAndDeviceIdAndLocalIdIn(any(), any(), anyList()))
            .thenReturn(List.of());
        doAnswer(i -> i.getArgument(0)).when(stockLogRepository).saveAll(any());

        service.pushData(TENANT_ID, List.of(sl));

        verify(itemVariantRepository).recalculateStock(50L);
    }

    @Test
    void push_multipleLogsForSameItem_recalculatesOnce() {
        MenuItem mi = menuItem(100L);
        StockLog sl1 = stockLog(1, 1000L, 5, null);
        sl1.setLocalId(1);
        StockLog sl2 = stockLog(2, 2000L, 5, null);
        sl2.setLocalId(2);

        when(menuItemRepository.findByRestaurantIdAndDeviceIdAndLocalId(any(), any(), anyInt()))
            .thenReturn(Optional.of(mi));
        when(stockLogRepository.findByRestaurantIdAndDeviceIdAndLocalIdIn(any(), any(), anyList()))
            .thenReturn(List.of());
        doAnswer(i -> i.getArgument(0)).when(stockLogRepository).saveAll(any());

        service.pushData(TENANT_ID, List.of(sl1, sl2));

        
        verify(menuItemRepository, times(1)).recalculateStock(100L);
    }

    

    @Test
    void push_mixedBatch_failedAndSuccessfulPartitioned() {
        MenuItem mi = menuItem(100L);
        StockLog good = stockLog(1, 1000L, 5, null);
        good.setLocalId(1);
        StockLog bad = stockLog(2, 2000L, 99, null); 
        bad.setLocalId(2);

        when(menuItemRepository.findByRestaurantIdAndDeviceIdAndLocalId(TENANT_ID, DEVICE, 5))
            .thenReturn(Optional.of(mi));
        when(menuItemRepository.findByRestaurantIdAndDeviceIdAndLocalId(TENANT_ID, DEVICE, 99))
            .thenReturn(Optional.empty());
        when(menuItemRepository.findById(99L)).thenReturn(Optional.empty());
        when(stockLogRepository.findByRestaurantIdAndDeviceIdAndLocalIdIn(any(), any(), anyList()))
            .thenReturn(List.of());
        doAnswer(i -> i.getArgument(0)).when(stockLogRepository).saveAll(any());

        PushSyncResponse resp = service.pushData(TENANT_ID, List.of(good, bad));

        assertThat(resp.getSuccessfulLocalIds()).contains(1);
        assertThat(resp.getFailedLocalIds()).contains(2);
    }

    

    private StockLog stockLog(int localId, long updatedAt, int menuItemId, Integer variantId) {
        StockLog sl = new StockLog();
        sl.setLocalId(localId);
        sl.setUpdatedAt(updatedAt);
        sl.setDeviceId(DEVICE);
        sl.setRestaurantId(TENANT_ID);
        sl.setMenuItemId(menuItemId);
        sl.setVariantId(variantId);
        sl.setReason("sale");
        sl.setDelta(java.math.BigDecimal.ONE);
        return sl;
    }

    private MenuItem menuItem(Long serverId) {
        MenuItem mi = new MenuItem();
        mi.setId(serverId);
        mi.setRestaurantId(TENANT_ID);
        return mi;
    }

    private ItemVariant itemVariant(Long serverId, Long tenantId) {
        ItemVariant iv = new ItemVariant();
        iv.setId(serverId);
        iv.setRestaurantId(tenantId);
        return iv;
    }

    private void stubSyncService() {
        when(stockLogRepository.findByRestaurantIdAndDeviceIdAndLocalIdIn(any(), any(), anyList()))
            .thenReturn(List.of());
        doAnswer(i -> i.getArgument(0)).when(stockLogRepository).saveAll(any());
    }
}
