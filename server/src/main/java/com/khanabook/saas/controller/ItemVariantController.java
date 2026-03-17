package com.khanabook.saas.controller;

import com.khanabook.saas.entity.ItemVariant;
import com.khanabook.saas.service.ItemVariantService;
import com.khanabook.saas.sync.dto.PushSyncResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.khanabook.saas.security.TenantContext;

@RestController
@RequestMapping("/sync/itemvariant")
@RequiredArgsConstructor
public class ItemVariantController {
	private final ItemVariantService service;

	@PostMapping("/push")
	public ResponseEntity<PushSyncResponse> push(@RequestBody List<ItemVariant> payload) {

		return ResponseEntity.ok(service.pushData(TenantContext.getCurrentTenant(), payload));
	}

	@GetMapping("/pull")
	public ResponseEntity<List<ItemVariant>> pull(@RequestParam Long lastSyncTimestamp, @RequestParam String deviceId) {
		return ResponseEntity.ok(service.pullData(TenantContext.getCurrentTenant(), lastSyncTimestamp, deviceId));
	}
}
