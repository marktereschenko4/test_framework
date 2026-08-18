package com.fintech.framework.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.microsoft.playwright.APIResponse;

public final class JsonUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private JsonUtils() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static JsonNode read(APIResponse response) {
        try {
            return MAPPER.readTree(response.text());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Response body is not valid JSON: " + response.text(), e);
        }
    }

    public static String pretty(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to serialize value to JSON", e);
        }
    }
}
