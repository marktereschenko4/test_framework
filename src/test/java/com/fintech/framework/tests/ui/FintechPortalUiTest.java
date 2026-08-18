package com.fintech.framework.tests.ui;

import com.fintech.framework.base.UiTestBase;
import com.fintech.framework.data.TestDataFactory;
import com.fintech.framework.models.UserRequest;
import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class FintechPortalUiTest extends UiTestBase {
    @Test
    void userRegistrationFlowCreatesUserAndPopulatesTransactionForm() {
        UserRequest user = TestDataFactory.premiumUser();

        page.navigate("/");
        registerUser(user);

        assertThat(page.locator("[data-testid='registration-message']")).containsText("Created user user-");
        String userId = page.locator("[data-testid='transaction-user-id-input']").inputValue();
        Assertions.assertTrue(userId.startsWith("user-"), "Expected created user id to populate transaction form");
    }

    @Test
    void transactionCreationFlowShowsSuccessMessage() {
        UserRequest user = TestDataFactory.premiumUser();

        page.navigate("/");
        registerUser(user);
        page.locator("[data-testid='amount-input']").fill("100.50");
        page.locator("[data-testid='recipient-id-input']").fill("recipient-456");
        page.locator("[data-testid='transaction-button']").click();

        assertThat(page.locator("[data-testid='transaction-message']")).containsText("Created transaction txn-");
    }

    @Test
    void transactionFlowShowsErrorForUnknownUser() {
        page.navigate("/");
        page.locator("[data-testid='transaction-user-id-input']").fill("user-does-not-exist");
        page.locator("[data-testid='amount-input']").fill("100.50");
        page.locator("[data-testid='recipient-id-input']").fill("recipient-456");
        page.locator("[data-testid='transaction-button']").click();

        Locator errorMessage = page.locator("[data-testid='transaction-message']");
        assertThat(errorMessage).containsText("User not found");
        Assertions.assertTrue(errorMessage.getAttribute("class").contains("error"));
    }

    private void registerUser(UserRequest user) {
        page.locator("[data-testid='name-input']").fill(user.name());
        page.locator("[data-testid='email-input']").fill(user.email());
        page.locator("[data-testid='account-type-select']").selectOption(user.accountType());
        page.locator("[data-testid='register-button']").click();
    }
}
