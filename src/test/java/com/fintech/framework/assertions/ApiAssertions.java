package com.fintech.framework.assertions;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.playwright.APIResponse;
import org.junit.jupiter.api.Assertions;

public final class ApiAssertions {
    private ApiAssertions() {
    }

    public static void assertStatus(APIResponse response, int expectedStatus) {
        Assertions.assertEquals(expectedStatus, response.status(), () -> "Unexpected API status. Body: " + response.text());
    }

    public static void assertJsonFieldEquals(JsonNode json, String field, String expectedValue) {
        Assertions.assertTrue(json.hasNonNull(field), "Expected JSON field to exist: " + field);
        Assertions.assertEquals(expectedValue, json.get(field).asText(), "Unexpected value for JSON field: " + field);
    }

    public static void assertJsonFieldPresent(JsonNode json, String field) {
        Assertions.assertTrue(json.hasNonNull(field), "Expected JSON field to exist: " + field);
    }

    public static void assertValidationError(JsonNode json, String expectedMessagePart) {
        Assertions.assertTrue(json.hasNonNull("error"), "Expected validation error response");
        Assertions.assertTrue(
                json.get("error").asText().contains(expectedMessagePart),
                () -> "Expected error to contain '" + expectedMessagePart + "' but was: " + json.get("error").asText());
    }
}
