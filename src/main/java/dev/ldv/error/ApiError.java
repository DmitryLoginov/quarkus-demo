package dev.ldv.error;

import java.util.Arrays;

public record ApiError(String code, String message, Violation[] violations) {

    public static final Violation[] EMPTY_VIOLATIONS = new Violation[0];

    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String NOT_FOUND = "NOT_FOUND";

    @Override
    public String toString() {
        return "ApiError{" +
                "code='" + code + '\'' +
                ", message='" + message + '\'' +
                ", violations=" + Arrays.toString(violations) +
                '}';
    }

    public record Violation(String field, String message) {
    }
}
