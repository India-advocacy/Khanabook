package com.khanabook.saas.controller;

import com.khanabook.saas.entity.User;
import com.khanabook.saas.service.UserService;
import com.khanabook.saas.sync.dto.PushSyncResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import com.khanabook.saas.security.TenantContext;

@RestController
@RequestMapping("/sync/config/users")
@RequiredArgsConstructor
public class UserController {
	private final UserService service;

	@PostMapping("/push")
	public ResponseEntity<PushSyncResponse> push(@RequestBody List<User> payload) {

		return ResponseEntity.ok(service.pushData(TenantContext.getCurrentTenant(), payload));
	}

	@GetMapping("/pull")
	public ResponseEntity<List<User>> pull(@RequestParam Long lastSyncTimestamp, @RequestParam String deviceId) {
		return ResponseEntity.ok(service.pullData(TenantContext.getCurrentTenant(), lastSyncTimestamp, deviceId));
	}

	@PostMapping("/update-mobile")
	public ResponseEntity<?> updateMobileNumber(@Valid @RequestBody UpdateMobileRequest request) {
		service.updateMobileNumber(TenantContext.getCurrentTenant(), request.getNewMobileNumber());
		return ResponseEntity.ok(Map.of("message", "Mobile number updated successfully."));
	}
}
