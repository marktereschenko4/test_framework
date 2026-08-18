package com.fintech.framework.utils;

import com.microsoft.playwright.APIResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ReportArtifacts {
    private static final Path API_LOG_DIR = Path.of("target", "api-responses");

    private ReportArtifacts() {
    }

    public static void logApiResponse(String name, APIResponse response) {
        try {
            Files.createDirectories(API_LOG_DIR);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("timestamp", Instant.now().toString());
            entry.put("name", name);
            entry.put("url", response.url());
            entry.put("status", response.status());
            entry.put("statusText", response.statusText());
            entry.put("body", response.text());

            Path logFile = API_LOG_DIR.resolve(sanitize(name) + "-" + System.nanoTime() + ".json");
            Files.writeString(logFile, JsonUtils.pretty(entry), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write API response artifact", e);
        }
    }

    public static Path screenshotPath(String testName) {
        try {
            Path directory = Path.of("target", "playwright-screenshots");
            Files.createDirectories(directory);
            return directory.resolve(sanitize(testName) + "-" + System.nanoTime() + ".png");
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create screenshot directory", e);
        }
    }

    private static String sanitize(String value) {
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
