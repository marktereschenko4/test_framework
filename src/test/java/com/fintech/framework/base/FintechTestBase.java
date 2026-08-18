package com.fintech.framework.base;

import com.fintech.framework.config.EnvironmentConfig;
import com.fintech.framework.mock.MockFintechApp;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public abstract class FintechTestBase {
    protected static EnvironmentConfig config;
    protected static MockFintechApp mockApp;
    protected static String baseUrl;
    protected static Playwright playwright;

    @BeforeAll
    static void startFramework() {
        config = EnvironmentConfig.load();
        if (config.useMockServer()) {
            mockApp = MockFintechApp.start(config.authToken());
            baseUrl = mockApp.baseUrl();
        } else {
            baseUrl = config.baseUrl();
        }
        playwright = Playwright.create();
    }

    @AfterAll
    static void stopFramework() {
        if (playwright != null) {
            playwright.close();
        }
        if (mockApp != null) {
            mockApp.close();
        }
    }
}
