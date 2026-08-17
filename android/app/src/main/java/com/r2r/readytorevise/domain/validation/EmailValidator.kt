package com.r2r.readytorevise.domain.validation


class EmailValidator {

    companion object {
        private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-zA-Z]{2,}\$".toRegex()
    }

    fun validate(email: String): ValidationResult {
        if (email.isBlank()) {
            return ValidationResult.Error("Email is required")
        }

        if (!EMAIL_REGEX.matches(email)) {
            return ValidationResult.Error("Invalid email")
        }

        return ValidationResult.Success
    }
}