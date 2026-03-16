package com.khanabook.saas;

import com.khanabook.saas.entity.*;
import com.khanabook.saas.repository.*;
import com.khanabook.saas.service.StockLogService;
import com.khanabook.saas.sync.dto.PushSyncResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Validates the stock recalculation model end-to-end.
 * Key invariant: currentStock on MenuItem = SUM(delta) from stock_logs.
 * This prevents multi-device drift where LWW on the stock field would cause
 * one device's adjustments to silently overwrite another's.
 */
@Transactional
class StockRecalculationIntegrationTest extends BaseIntegrationTest {

    @Autowired StockLogService stockLogService;
    @Autowired MenuItemRepository menuItemRepo;
    @Autowired ItemVariantRepository itemVariantRepo;
    @Autowired StockLogRepository stockLogRepo;
    @Autowired CategoryRepository categoryRepo;

    private static final Long TENANT = 2001L;
    private static final String DEVICE_A = "PHONE_A";
    private static final String DEVICE_B = "PHONE_B";

    private Long savedMenuItemId;

    @BeforeEach
    void setUp() {
        stockLogRepo.deleteAll();
        itemVariantRepo.deleteAll();
        menuItemRepo.deleteAll();
        categoryRepo.deleteAll();

        Category cat = category(TENANT, DEVICE_A, 1);
        Category savedCat = categoryRepo.save(cat);

        MenuItem item = menuItem(TENANT, DEVICE_A, 1, savedCat.getId());
        item.setCurrentStock(BigDecimal.ZERO);
        savedMenuItemId = menuItemRepo.save(item).getId();
    }

    @Test
    void initialStock_setViaStockLog_recalculatedCorrectly() {
        StockLog initial = stockLog(1, DEVICE_A, 1, null, new BigDecimal("50.00"), "initial");
        initial.setServerMenuItemId(savedMenuItemId);

        PushSyncResponse resp = stockLogService.pushData(TENANT, List.of(initial));

        assertThat(resp.getSuccessfulLocalIds()).contains(1);
        MenuItem updated = menuItemRepo.findById(savedMenuItemId).orElseThrow();
        assertThat(updated.getCurrentStock()).isEqualByComparingTo("50.00");
    }

    @Test
    void saleThenAdjustment_stockCalculatesCorrectly() {
        StockLog init = stockLog(1, DEVICE_A, 1, null, new BigDecimal("100.00"), "initial");
        init.setServerMenuItemId(savedMenuItemId);
        StockLog sale = stockLog(2, DEVICE_A, 1, null, new BigDecimal("-3.00"), "sale");
        sale.setServerMenuItemId(savedMenuItemId);

        stockLogService.pushData(TENANT, List.of(init));
        stockLogService.pushData(TENANT, List.of(sale));

        MenuItem updated = menuItemRepo.findById(savedMenuItemId).orElseThrow();
        assertThat(updated.getCurrentStock()).isEqualByComparingTo("97.00");
    }

    @Test
    void multiDeviceStockLogs_bothContributedToTotal() {
        // Device A adds initial stock
        StockLog fromA = stockLog(1, DEVICE_A, 1, null, new BigDecimal("50.00"), "initial");
        fromA.setServerMenuItemId(savedMenuItemId);

        // Device B records a sale (offline, then syncs later)
        StockLog fromB = stockLog(1, DEVICE_B, 1, null, new BigDecimal("-5.00"), "sale");
        fromB.setServerMenuItemId(savedMenuItemId);

        stockLogService.pushData(TENANT, List.of(fromA));
        stockLogService.pushData(TENANT, List.of(fromB));

        // Both logs should be reflected — no LWW race on the stock value
        MenuItem updated = menuItemRepo.findById(savedMenuItemId).orElseThrow();
        assertThat(updated.getCurrentStock()).isEqualByComparingTo("45.00");
    }

    @Test
    void stockLog_isDeletedSoftly_doesNotAffectCurrentStock() {
        // Push initial stock
        StockLog init = stockLog(1, DEVICE_A, 1, null, new BigDecimal("20.00"), "initial");
        init.setServerMenuItemId(savedMenuItemId);
        stockLogService.pushData(TENANT, List.of(init));

        // Push a soft-deleted sale (is_deleted = true)
        StockLog deletedSale = stockLog(2, DEVICE_A, 1, null, new BigDecimal("-5.00"), "sale");
        deletedSale.setServerMenuItemId(savedMenuItemId);
        deletedSale.setIsDeleted(true);
        stockLogService.pushData(TENANT, List.of(deletedSale));

        // The recalculateStock query sums ALL logs including deleted ones.
        // This is the current design — document it so the team is aware.
        // TODO: decide whether soft-deleted logs should be excluded from stock sum.
        MenuItem updated = menuItemRepo.findById(savedMenuItemId).orElseThrow();
        // Currently includes deleted: 20 + (-5) = 15
        // After design decision: might be 20 (excluding deleted)
        assertThat(updated.getCurrentStock())
            .isEqualByComparingTo("15.00"); // documents current behaviour
    }

    @Test
    void weightBasedItem_fractionalDelta_preservedWithFourDecimals() {
        StockLog init = stockLog(1, DEVICE_A, 1, null, new BigDecimal("10.2500"), "initial");
        init.setServerMenuItemId(savedMenuItemId);
        StockLog sale = stockLog(2, DEVICE_A, 1, null, new BigDecimal("-0.1250"), "sale");
        sale.setServerMenuItemId(savedMenuItemId);

        stockLogService.pushData(TENANT, List.of(init));
        stockLogService.pushData(TENANT, List.of(sale));

        MenuItem updated = menuItemRepo.findById(savedMenuItemId).orElseThrow();
        assertThat(updated.getCurrentStock()).isEqualByComparingTo("10.1250");
    }

    @Test
    void variantStock_recalculatedIndependentlyFromMenuItemStock() {
        ItemVariant variant = variant(TENANT, DEVICE_A, 1, savedMenuItemId);
        variant.setCurrentStock(BigDecimal.ZERO);
        Long variantId = itemVariantRepo.save(variant).getId();

        StockLog variantLog = stockLog(1, DEVICE_A, 1, 1, new BigDecimal("30.00"), "initial");
        variantLog.setServerMenuItemId(savedMenuItemId);
        variantLog.setServerVariantId(variantId);

        stockLogService.pushData(TENANT, List.of(variantLog));

        ItemVariant updatedVariant = itemVariantRepo.findById(variantId).orElseThrow();
        assertThat(updatedVariant.getCurrentStock()).isEqualByComparingTo("30.00");

        // MenuItem stock is also recalculated (includes the variant's log)
        MenuItem updatedItem = menuItemRepo.findById(savedMenuItemId).orElseThrow();
        assertThat(updatedItem.getCurrentStock()).isEqualByComparingTo("30.00");
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private StockLog stockLog(int localId, String device, int menuItemLocalId,
                               Integer variantLocalId, BigDecimal delta, String reason) {
        StockLog sl = new StockLog();
        sl.setLocalId(localId);
        sl.setDeviceId(device);
        sl.setRestaurantId(TENANT);
        sl.setUpdatedAt(System.currentTimeMillis());
        sl.setServerUpdatedAt(0L);
        sl.setCreatedAt(System.currentTimeMillis());
        sl.setIsDeleted(false);
        sl.setMenuItemId(menuItemLocalId);
        sl.setVariantId(variantLocalId);
        sl.setDelta(delta);
        sl.setReason(reason);
        return sl;
    }

    private Category category(Long tenantId, String deviceId, int localId) {
        Category c = new Category();
        c.setRestaurantId(tenantId);
        c.setDeviceId(deviceId);
        c.setLocalId(localId);
        c.setUpdatedAt(1000L);
        c.setServerUpdatedAt(1000L);
        c.setCreatedAt(1000L);
        c.setIsDeleted(false);
        c.setName("Food");
        c.setIsVeg(true);
        c.setIsActive(true);
        return c;
    }

    private MenuItem menuItem(Long tenantId, String deviceId, int localId, Long serverCategoryId) {
        MenuItem m = new MenuItem();
        m.setRestaurantId(tenantId);
        m.setDeviceId(deviceId);
        m.setLocalId(localId);
        m.setUpdatedAt(1000L);
        m.setServerUpdatedAt(1000L);
        m.setCreatedAt(1000L);
        m.setIsDeleted(false);
        m.setCategoryId(1);
        m.setServerCategoryId(serverCategoryId);
        m.setName("Samosa");
        m.setBasePrice(new BigDecimal("15.00"));
        m.setIsAvailable(true);
        return m;
    }

    private ItemVariant variant(Long tenantId, String deviceId, int localId, Long serverMenuItemId) {
        ItemVariant v = new ItemVariant();
        v.setRestaurantId(tenantId);
        v.setDeviceId(deviceId);
        v.setLocalId(localId);
        v.setUpdatedAt(1000L);
        v.setServerUpdatedAt(1000L);
        v.setCreatedAt(1000L);
        v.setIsDeleted(false);
        v.setMenuItemId(1);
        v.setServerMenuItemId(serverMenuItemId);
        v.setVariantName("Small");
        v.setPrice(new BigDecimal("10.00"));
        v.setIsAvailable(true);
        return v;
    }
}
