package com.khanabook.saas.controller;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateMobileRequest {
    @NotBlank(message = "New mobile number cannot be empty")
    private String newMobileNumber;
}