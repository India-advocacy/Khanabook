package com.khanabook.saas.controller;

import com.khanabook.saas.entity.StockLog;
import com.khanabook.saas.service.StockLogService;
import com.khanabook.saas.sync.dto.PushSyncResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.khanabook.saas.security.TenantContext;

@RestController
@RequestMapping("/sync/stocklog")
@RequiredArgsConstructor
public class StockLogController {
	private final StockLogService service;

	@PostMapping("/push")
	public ResponseEntity<PushSyncResponse> push(@RequestBody List<StockLog> payload) {

		return ResponseEntity.ok(service.pushData(TenantContext.getCurrentTenant(), payload));
	}

	@GetMapping("/pull")
	public ResponseEntity<List<StockLog>> pull(@RequestParam Long lastSyncTimestamp, @RequestParam String deviceId) {
		return ResponseEntity.ok(service.pullData(TenantContext.getCurrentTenant(), lastSyncTimestamp, deviceId));
	}
}
