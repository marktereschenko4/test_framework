package com.fintech.framework.models;

import java.math.BigDecimal;

public record TransactionRequest(String userId, BigDecimal amount, String type, String recipientId) {
}
