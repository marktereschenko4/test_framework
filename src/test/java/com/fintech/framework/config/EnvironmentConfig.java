package com.fintech.framework.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

public final class EnvironmentConfig {
    private final String environment;
    private final String baseUrl;
    private final boolean useMockServer;
    private final String authToken;
    private final int apiTimeoutMs;
    private final int uiDefaultTimeoutMs;
    private final String browser;
    private final boolean headless;

    private EnvironmentConfig(
            String environment,
            String baseUrl,
            boolean useMockServer,
            String authToken,
            int apiTimeoutMs,
            int uiDefaultTimeoutMs,
            String browser,
            boolean headless) {
        this.environment = environment;
        this.baseUrl = baseUrl;
        this.useMockServer = useMockServer;
        this.authToken = authToken;
        this.apiTimeoutMs = apiTimeoutMs;
        this.uiDefaultTimeoutMs = uiDefaultTimeoutMs;
        this.browser = browser;
        this.headless = headless;
    }

    public static EnvironmentConfig load() {
        String env = System.getProperty("env", "local");
        Properties properties = new Properties();
        String resourcePath = "environments/" + env + ".properties";

        try (InputStream stream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IllegalArgumentException("Missing environment config: " + resourcePath);
            }
            properties.load(stream);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load environment config: " + resourcePath, e);
        }

        String authToken = resolveEnvPlaceholder(required(properties, "api.authToken"));
        return new EnvironmentConfig(
                env,
                required(properties, "app.baseUrl"),
                Boolean.parseBoolean(required(properties, "app.useMockServer")),
                authToken,
                Integer.parseInt(required(properties, "api.timeoutMs")),
                Integer.parseInt(required(properties, "ui.defaultTimeoutMs")),
                System.getProperty("browser", "chromium"),
                Boolean.parseBoolean(System.getProperty("headless", "true")));
    }

    private static String required(Properties properties, String key) {
        return Objects.requireNonNull(properties.getProperty(key), "Missing required config key: " + key).trim();
    }

    private static String resolveEnvPlaceholder(String value) {
        if (value.startsWith("${") && value.endsWith("}")) {
            String envVar = value.substring(2, value.length() - 1);
            String resolved = System.getenv(envVar);
            if (resolved == null || resolved.isBlank()) {
                throw new IllegalStateException("Missing environment variable required by config: " + envVar);
            }
            return resolved;
        }
        return value;
    }

    public String environment() {
        return environment;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public boolean useMockServer() {
        return useMockServer;
    }

    public String authToken() {
        return authToken;
    }

    public int apiTimeoutMs() {
        return apiTimeoutMs;
    }

    public int uiDefaultTimeoutMs() {
        return uiDefaultTimeoutMs;
    }

    public String browser() {
        return browser;
    }

    public boolean headless() {
        return headless;
    }
}
