package com.fintech.framework.api;

import com.fintech.framework.utils.JsonUtils;
import com.fintech.framework.utils.ReportArtifacts;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;

public final class ApiClient {
    private final APIRequestContext request;

    public ApiClient(APIRequestContext request) {
        this.request = request;
    }

    public APIResponse get(String path) {
        APIResponse response = request.get(path);
        ReportArtifacts.logApiResponse("GET " + path, response);
        return response;
    }

    public APIResponse post(String path, Object body) {
        APIResponse response = request.post(path, RequestOptions.create().setData(JsonUtils.pretty(body)));
        ReportArtifacts.logApiResponse("POST " + path, response);
        return response;
    }

    public APIResponse put(String path, Object body) {
        APIResponse response = request.put(path, RequestOptions.create().setData(JsonUtils.pretty(body)));
        ReportArtifacts.logApiResponse("PUT " + path, response);
        return response;
    }

    public APIResponse delete(String path) {
        APIResponse response = request.delete(path);
        ReportArtifacts.logApiResponse("DELETE " + path, response);
        return response;
    }
}
