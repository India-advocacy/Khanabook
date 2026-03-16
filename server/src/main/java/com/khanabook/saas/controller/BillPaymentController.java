package com.khanabook.saas.controller;

import com.khanabook.saas.entity.BillPayment;
import com.khanabook.saas.service.BillPaymentService;
import com.khanabook.saas.sync.dto.PushSyncResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.khanabook.saas.security.TenantContext;

@RestController
@RequestMapping("/sync/bills/payments")
@RequiredArgsConstructor
public class BillPaymentController {
    private final BillPaymentService service;

    @PostMapping("/push")
    public ResponseEntity<PushSyncResponse> push(@RequestBody List<BillPayment> payload) {
        // TenantId automatically extracted by JwtFilter
        return ResponseEntity.ok(service.pushData(TenantContext.getCurrentTenant(), payload));
    }

    @GetMapping("/pull")
    public ResponseEntity<List<BillPayment>> pull(
            @RequestParam Long lastSyncTimestamp,
            @RequestParam String deviceId) {
        return ResponseEntity.ok(service.pullData(TenantContext.getCurrentTenant(), lastSyncTimestamp, deviceId));
    }
}
