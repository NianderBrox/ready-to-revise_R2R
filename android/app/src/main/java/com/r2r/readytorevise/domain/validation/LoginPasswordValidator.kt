package com.r2r.readytorevise.domain.validation

class LoginPasswordValidator {

    fun validate(password: String): ValidationResult {

        if (password.isBlank()) {
            return ValidationResult.Error("Password is required")
        }

        return ValidationResult.Success
    }
}