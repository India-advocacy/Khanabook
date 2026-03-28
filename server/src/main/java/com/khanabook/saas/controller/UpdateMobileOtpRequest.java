package com.khanabook.saas.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateMobileOtpRequest {
	@NotBlank(message = "New mobile number cannot be empty")
	@Pattern(regexp = "^\\+?[1-9]\\d{6,19}$", message = "Phone number must be valid format")
	@Size(max = 20)
	private String newMobileNumber;
}
