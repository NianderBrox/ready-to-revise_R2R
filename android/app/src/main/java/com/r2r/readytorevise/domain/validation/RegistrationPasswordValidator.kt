package com.r2r.readytorevise.domain.validation

class RegistrationPasswordValidator {

    fun validate(password: String): ValidationResult {

        if (password.isBlank()) {
            return ValidationResult.Error("Password is required")
        }

        if (password.length < 8) {
            return ValidationResult.Error(
                "Password must be at least 8 characters long"
            )
        }

        if (!password.any(Char::isUpperCase)) {
            return ValidationResult.Error(
                "Password must contain at least one uppercase letter"
            )
        }

        if (!password.any(Char::isLowerCase)) {
            return ValidationResult.Error(
                "Password must contain at least one lowercase letter"
            )
        }

        if (!password.any(Char::isDigit)) {
            return ValidationResult.Error(
                "Password must contain at least one number"
            )
        }

        if (!password.any { !it.isLetterOrDigit() }) {
            return ValidationResult.Error(
                "Password must contain at least one special character"
            )
        }

        return ValidationResult.Success
    }
}