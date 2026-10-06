package com.anurag.ECE.dto;

import com.anurag.ECE.entity.UserRole;
import jakarta.validation.constraints.*;

public record RegistrationRequest(
        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 80, message = "Name must be 2 to 80 characters") String fullName,
        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address") @Size(max = 120) String email,
        @Pattern(regexp = "^$|^[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian mobile number") String phone,
        @Size(max = 100, message = "Organization must be at most 100 characters") String organization,
        @NotNull(message = "Select a role") UserRole role,
        @NotBlank(message = "Password is required")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,64}$",
                message = "Password needs 8+ characters with upper case, lower case, a digit and a symbol")
        String password,
        @NotBlank(message = "Confirm your password") String confirmPassword,
        @AssertTrue(message = "You must accept the terms of use") boolean acceptedTerms) {

    /** Cross-field rule; reported under the field name "passwordsMatching". */
    @AssertTrue(message = "Passwords do not match")
    public boolean isPasswordsMatching() {
        return password == null || password.equals(confirmPassword);
    }
}
