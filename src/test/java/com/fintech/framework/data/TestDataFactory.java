package com.fintech.framework.data;

import com.fintech.framework.models.TransactionRequest;
import com.fintech.framework.models.UserRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class TestDataFactory {
    private TestDataFactory() {
    }

    public static UserRequest premiumUser() {
        return user("premium");
    }

    public static UserRequest standardUser() {
        return user("standard");
    }

    public static UserRequest user(String accountType) {
        String unique = UUID.randomUUID().toString().substring(0, 8).toLowerCase(Locale.ROOT);
        return new UserRequest("Auto User " + unique, "auto." + unique + "@example.com", accountType);
    }

    public static UserRequest invalidUserMissingEmail() {
        return new UserRequest("Missing Email", "", "premium");
    }

    public static TransactionRequest transfer(String userId, String recipientId) {
        return new TransactionRequest(userId, money(25, 500), "transfer", recipientId);
    }

    public static TransactionRequest invalidNegativeTransfer(String userId, String recipientId) {
        return new TransactionRequest(userId, new BigDecimal("-10.00"), "transfer", recipientId);
    }

    private static BigDecimal money(int min, int max) {
        double value = ThreadLocalRandom.current().nextDouble(min, max);
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
