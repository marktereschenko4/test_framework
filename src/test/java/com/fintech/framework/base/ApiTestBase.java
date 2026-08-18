package com.fintech.framework.base;

import com.fintech.framework.api.ApiClient;
import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class ApiTestBase extends FintechTestBase {
    protected APIRequestContext apiRequest;
    protected ApiClient api;

    @BeforeEach
    void createApiContext() {
        apiRequest = newApiContext(true);
        api = new ApiClient(apiRequest);
    }

    @AfterEach
    void disposeApiContext() {
        if (apiRequest != null) {
            apiRequest.dispose();
        }
    }

    protected APIRequestContext newApiContext(boolean authenticated) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");
        if (authenticated) {
            headers.put("Authorization", "Bearer " + config.authToken());
        }

        return playwright.request().newContext(new APIRequest.NewContextOptions()
                .setBaseURL(baseUrl)
                .setExtraHTTPHeaders(headers)
                .setTimeout(config.apiTimeoutMs()));
    }
}
