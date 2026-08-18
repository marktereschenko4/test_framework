package com.fintech.framework.tests.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fintech.framework.api.ApiClient;
import com.fintech.framework.assertions.ApiAssertions;
import com.fintech.framework.base.ApiTestBase;
import com.fintech.framework.data.TestDataFactory;
import com.fintech.framework.models.UserRequest;
import com.fintech.framework.utils.JsonUtils;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import org.junit.jupiter.api.Test;

public class UserApiTest extends ApiTestBase {
    @Test
    void createAndReadUser() {
        UserRequest user = TestDataFactory.premiumUser();

        APIResponse createResponse = api.post("/api/users", user);
        ApiAssertions.assertStatus(createResponse, 201);
        JsonNode createdUser = JsonUtils.read(createResponse);
        ApiAssertions.assertJsonFieldPresent(createdUser, "id");
        ApiAssertions.assertJsonFieldEquals(createdUser, "name", user.name());
        ApiAssertions.assertJsonFieldEquals(createdUser, "email", user.email());
        ApiAssertions.assertJsonFieldEquals(createdUser, "accountType", "premium");

        APIResponse getResponse = api.get("/api/users/" + createdUser.get("id").asText());
        ApiAssertions.assertStatus(getResponse, 200);
        JsonNode fetchedUser = JsonUtils.read(getResponse);
        ApiAssertions.assertJsonFieldEquals(fetchedUser, "id", createdUser.get("id").asText());
        ApiAssertions.assertJsonFieldEquals(fetchedUser, "email", user.email());
    }

    @Test
    void updateAndDeleteUserForCrudCoverage() {
        JsonNode createdUser = JsonUtils.read(api.post("/api/users", TestDataFactory.premiumUser()));
        String userId = createdUser.get("id").asText();
        UserRequest update = TestDataFactory.standardUser();

        APIResponse updateResponse = api.put("/api/users/" + userId, update);
        ApiAssertions.assertStatus(updateResponse, 200);
        JsonNode updatedUser = JsonUtils.read(updateResponse);
        ApiAssertions.assertJsonFieldEquals(updatedUser, "id", userId);
        ApiAssertions.assertJsonFieldEquals(updatedUser, "accountType", "standard");
        ApiAssertions.assertJsonFieldEquals(updatedUser, "email", update.email());

        APIResponse deleteResponse = api.delete("/api/users/" + userId);
        ApiAssertions.assertStatus(deleteResponse, 204);

        APIResponse getDeletedResponse = api.get("/api/users/" + userId);
        ApiAssertions.assertStatus(getDeletedResponse, 404);
        ApiAssertions.assertValidationError(JsonUtils.read(getDeletedResponse), "User not found");
    }

    @Test
    void rejectInvalidUserData() {
        APIResponse response = api.post("/api/users", TestDataFactory.invalidUserMissingEmail());

        ApiAssertions.assertStatus(response, 400);
        ApiAssertions.assertValidationError(JsonUtils.read(response), "valid email is required");
    }

    @Test
    void requireAuthorizationForUserApis() {
        APIRequestContext unauthenticatedContext = newApiContext(false);
        try {
            ApiClient unauthenticatedApi = new ApiClient(unauthenticatedContext);

            APIResponse response = unauthenticatedApi.post("/api/users", TestDataFactory.premiumUser());
            ApiAssertions.assertStatus(response, 401);
            ApiAssertions.assertValidationError(JsonUtils.read(response), "bearer token");
        } finally {
            unauthenticatedContext.dispose();
        }
    }
}
