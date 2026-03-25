package com.khanabook.saas.service.impl;

import com.khanabook.saas.entity.User;
import com.khanabook.saas.repository.UserRepository;
import com.khanabook.saas.service.UserService;
import com.khanabook.saas.sync.dto.PushSyncResponse;
import com.khanabook.saas.sync.service.GenericSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
	private final UserRepository repository;
	private final GenericSyncService genericSyncService;

	@Override
	public PushSyncResponse pushData(Long tenantId, List<User> payload) {
		return genericSyncService.handlePushSync(tenantId, payload, repository);
	}

	@Override
	public List<User> pullData(Long tenantId, Long lastSyncTimestamp, String deviceId) {
		return repository.findByRestaurantIdAndServerUpdatedAtGreaterThanAndDeviceIdNot(tenantId, lastSyncTimestamp,
				deviceId);
	}

	@Override
	@Transactional
	public void updateMobileNumber(Long tenantId, String newMobileNumber) {
		// Check if anyone else is using this number as their email OR whatsapp number
		Optional<User> existingUserByEmail = repository.findByEmail(newMobileNumber);
		if (existingUserByEmail.isPresent()) {
			User existingUser = existingUserByEmail.get();
			if (!existingUser.getRestaurantId().equals(tenantId)) {
				throw new IllegalArgumentException("This number is already related to another shop.");
			}
		}

		Optional<User> existingUserByWhatsapp = repository.findByWhatsappNumber(newMobileNumber);
		if (existingUserByWhatsapp.isPresent()) {
			User existingUser = existingUserByWhatsapp.get();
			if (!existingUser.getRestaurantId().equals(tenantId)) {
				throw new IllegalArgumentException("This number is already related to another shop.");
			}
		}

		List<User> users = repository.findByRestaurantIdAndLocalIdIn(tenantId, List.of(1L));
		if (users.isEmpty()) {
			throw new IllegalStateException("Primary user for restaurant not found.");
		}
		
		User currentUser = users.get(0);

		// If their current login ID (email) is also a phone number, update it.
		// If it's a real email address (e.g. from Google login), leave it alone.
		if (currentUser.getEmail() != null && !currentUser.getEmail().contains("@")) {
			currentUser.setEmail(newMobileNumber);
		}
		
		currentUser.setWhatsappNumber(newMobileNumber);
		currentUser.setUpdatedAt(System.currentTimeMillis());
		currentUser.setServerUpdatedAt(System.currentTimeMillis());
		
		repository.save(currentUser);
	}
}
