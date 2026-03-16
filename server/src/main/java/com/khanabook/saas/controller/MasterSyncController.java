package com.khanabook.saas.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.khanabook.saas.entity.*;
import com.khanabook.saas.security.TenantContext;
import com.khanabook.saas.service.*;

import lombok.RequiredArgsConstructor;
import lombok.Data;
import java.util.List;

@RestController
@RequestMapping("/sync/master")
@RequiredArgsConstructor
public class MasterSyncController {

  private final RestaurantProfileService restaurantProfileService;
  private final UserService userService;
  private final CategoryService categoryService;
  private final MenuItemService menuItemService;
  private final ItemVariantService itemVariantService;
  private final StockLogService stockLogService;
  private final BillService billService;
  private final BillItemService billItemService;
  private final BillPaymentService billPaymentService;

  @org.springframework.transaction.annotation.Transactional(readOnly = true)
  @GetMapping("/pull")
  public ResponseEntity<MasterSyncResponse> pullMasterSync(
      @RequestParam Long lastSyncTimestamp,
      @RequestParam String deviceId) {

    Long tenantId = TenantContext.getCurrentTenant();

    MasterSyncResponse response = new MasterSyncResponse();
    response.setProfiles(restaurantProfileService.pullData(tenantId, lastSyncTimestamp, deviceId));
    response.setUsers(userService.pullData(tenantId, lastSyncTimestamp, deviceId));
    response.setCategories(categoryService.pullData(tenantId, lastSyncTimestamp, deviceId));
    response.setMenuItems(menuItemService.pullData(tenantId, lastSyncTimestamp, deviceId));
    response.setItemVariants(itemVariantService.pullData(tenantId, lastSyncTimestamp, deviceId));
    response.setStockLogs(stockLogService.pullData(tenantId, lastSyncTimestamp, deviceId));
    response.setBills(billService.pullData(tenantId, lastSyncTimestamp, deviceId));
    response.setBillItems(billItemService.pullData(tenantId, lastSyncTimestamp, deviceId));
    response.setBillPayments(billPaymentService.pullData(tenantId, lastSyncTimestamp, deviceId));

    return ResponseEntity.ok(response);
  }

  @Data
  public static class MasterSyncResponse {
    private List<RestaurantProfile> profiles;
    private List<User> users;
    private List<Category> categories;
    private List<MenuItem> menuItems;
    private List<ItemVariant> itemVariants;
    private List<StockLog> stockLogs;
    private List<Bill> bills;
    private List<BillItem> billItems;
    private List<BillPayment> billPayments;
  }
}
