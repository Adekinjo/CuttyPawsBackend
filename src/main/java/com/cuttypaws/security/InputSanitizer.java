package com.cuttypaws.security;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class InputSanitizer {

    // Patterns to detect malicious input
    private static final Pattern SQL_INJECTION_PATTERN =
            Pattern.compile("('.+--|;|\\b(OR|AND)\\b.*=.*|\\b(SELECT|UPDATE|DELETE|INSERT|DROP|UNION)\\b)",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern XSS_PATTERN =
            Pattern.compile("<script|javascript:|on\\w+\\s*=|eval\\(|alert\\(|document\\.|window\\.",
                    Pattern.CASE_INSENSITIVE);

    // Check if input contains malicious content
    public boolean isMalicious(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }

        String lowerInput = input.toLowerCase();
        return SQL_INJECTION_PATTERN.matcher(lowerInput).find() ||
                XSS_PATTERN.matcher(lowerInput).find();
    }

    // Clean input by removing dangerous characters
    public String sanitize(String input) {
        if (input == null) return null;

        // Remove SQL injection attempts
        String cleaned = SQL_INJECTION_PATTERN.matcher(input).replaceAll("");

        // Remove XSS attempts
        cleaned = XSS_PATTERN.matcher(cleaned).replaceAll("");

        // Basic HTML escaping
        cleaned = cleaned.replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");

        return cleaned.trim();
    }

    // Validate email format
    public boolean isValidEmail(String email) {
        if (email == null || email.length() > 254) return false;
        return Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$").matcher(email).matches();
    }
}
