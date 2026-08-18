package com.fintech.framework.tests.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fintech.framework.api.ApiClient;
import com.fintech.framework.assertions.ApiAssertions;
import com.fintech.framework.base.ApiTestBase;
import com.fintech.framework.data.TestDataFactory;
import com.fintech.framework.models.TransactionRequest;
import com.fintech.framework.utils.JsonUtils;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TransactionApiTest extends ApiTestBase {
    @Test
    void createAndFetchUserTransactions() {
        String userId = createUser();
        TransactionRequest transaction = TestDataFactory.transfer(userId, "recipient-456");

        APIResponse createResponse = api.post("/api/transactions", transaction);
        ApiAssertions.assertStatus(createResponse, 201);
        JsonNode createdTransaction = JsonUtils.read(createResponse);
        ApiAssertions.assertJsonFieldPresent(createdTransaction, "id");
        ApiAssertions.assertJsonFieldEquals(createdTransaction, "userId", userId);
        ApiAssertions.assertJsonFieldEquals(createdTransaction, "type", "transfer");

        APIResponse listResponse = api.get("/api/transactions/" + userId);
        ApiAssertions.assertStatus(listResponse, 200);
        JsonNode transactionList = JsonUtils.read(listResponse);
        Assertions.assertEquals(1, transactionList.get("transactions").size(), "Expected one transaction for user");
        ApiAssertions.assertJsonFieldEquals(transactionList.get("transactions").get(0), "id", createdTransaction.get("id").asText());
    }

    @Test
    void rejectNegativeTransactionAmount() {
        String userId = createUser();
        TransactionRequest transaction = TestDataFactory.invalidNegativeTransfer(userId, "recipient-456");

        APIResponse response = api.post("/api/transactions", transaction);

        ApiAssertions.assertStatus(response, 400);
        ApiAssertions.assertValidationError(JsonUtils.read(response), "amount must be greater than zero");
    }

    @Test
    void rejectTransactionForUnknownUser() {
        TransactionRequest transaction = TestDataFactory.transfer("user-does-not-exist", "recipient-456");

        APIResponse response = api.post("/api/transactions", transaction);

        ApiAssertions.assertStatus(response, 404);
        ApiAssertions.assertValidationError(JsonUtils.read(response), "User not found");
    }

    @Test
    void requireAuthorizationForTransactionApis() {
        APIRequestContext unauthenticatedContext = newApiContext(false);
        try {
            ApiClient unauthenticatedApi = new ApiClient(unauthenticatedContext);
            TransactionRequest transaction = TestDataFactory.transfer("user-123", "recipient-456");

            APIResponse response = unauthenticatedApi.post("/api/transactions", transaction);
            ApiAssertions.assertStatus(response, 401);
            ApiAssertions.assertValidationError(JsonUtils.read(response), "bearer token");
        } finally {
            unauthenticatedContext.dispose();
        }
    }

    private String createUser() {
        APIResponse response = api.post("/api/users", TestDataFactory.premiumUser());
        ApiAssertions.assertStatus(response, 201);
        return JsonUtils.read(response).get("id").asText();
    }
}
