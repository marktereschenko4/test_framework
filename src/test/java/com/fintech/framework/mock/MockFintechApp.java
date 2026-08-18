package com.fintech.framework.mock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fintech.framework.models.TransactionRequest;
import com.fintech.framework.models.UserRequest;
import com.fintech.framework.utils.JsonUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public final class MockFintechApp implements AutoCloseable {
    private final HttpServer server;
    private final String authToken;
    private final Map<String, ObjectNode> users = new ConcurrentHashMap<>();
    private final Map<String, ObjectNode> transactions = new ConcurrentHashMap<>();
    private final AtomicInteger userSequence = new AtomicInteger(1000);
    private final AtomicInteger transactionSequence = new AtomicInteger(2000);

    private MockFintechApp(HttpServer server, String authToken) {
        this.server = server;
        this.authToken = authToken;
    }

    public static MockFintechApp start(String authToken) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
            MockFintechApp app = new MockFintechApp(server, authToken);
            server.createContext("/", app::handle);
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            return app;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to start mock fintech app", e);
        }
    }

    public String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                send(exchange, 204, "");
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if ("GET".equals(method) && ("/".equals(path) || "/ui".equals(path))) {
                sendHtml(exchange, uiHtml());
                return;
            }
            if ("GET".equals(method) && "/health".equals(path)) {
                sendJson(exchange, 200, Map.of("status", "ok"));
                return;
            }

            if (path.startsWith("/api/") && !isAuthorized(exchange)) {
                sendJson(exchange, 401, Map.of("error", "Missing or invalid bearer token"));
                return;
            }

            if ("POST".equals(method) && "/api/users".equals(path)) {
                createUser(exchange);
                return;
            }
            if ("GET".equals(method) && path.startsWith("/api/users/")) {
                getUser(exchange, path);
                return;
            }
            if ("PUT".equals(method) && path.startsWith("/api/users/")) {
                updateUser(exchange, path);
                return;
            }
            if ("DELETE".equals(method) && path.startsWith("/api/users/")) {
                deleteUser(exchange, path);
                return;
            }
            if ("POST".equals(method) && "/api/transactions".equals(path)) {
                createTransaction(exchange);
                return;
            }
            if ("GET".equals(method) && path.startsWith("/api/transactions/")) {
                getTransactions(exchange, path);
                return;
            }

            sendJson(exchange, 404, Map.of("error", "Route not found"));
        } catch (Exception e) {
            sendJson(exchange, 500, Map.of("error", e.getMessage()));
        }
    }

    private boolean isAuthorized(HttpExchange exchange) {
        List<String> values = exchange.getRequestHeaders().getOrDefault("Authorization", List.of());
        return values.stream().anyMatch(value -> value.equals("Bearer " + authToken));
    }

    private void createUser(HttpExchange exchange) throws IOException {
        UserRequest request = JsonUtils.mapper().readValue(requestBody(exchange), UserRequest.class);
        String validationError = validateUser(request);
        if (validationError != null) {
            sendJson(exchange, 400, Map.of("error", validationError));
            return;
        }

        String id = "user-" + userSequence.incrementAndGet();
        ObjectNode user = userJson(id, request.name(), request.email(), request.accountType());
        users.put(id, user);
        sendJson(exchange, 201, user);
    }

    private void getUser(HttpExchange exchange, String path) throws IOException {
        String id = tail(path, "/api/users/");
        ObjectNode user = users.get(id);
        if (user == null) {
            sendJson(exchange, 404, Map.of("error", "User not found"));
            return;
        }
        sendJson(exchange, 200, user);
    }

    private void updateUser(HttpExchange exchange, String path) throws IOException {
        String id = tail(path, "/api/users/");
        if (!users.containsKey(id)) {
            sendJson(exchange, 404, Map.of("error", "User not found"));
            return;
        }

        UserRequest request = JsonUtils.mapper().readValue(requestBody(exchange), UserRequest.class);
        String validationError = validateUser(request);
        if (validationError != null) {
            sendJson(exchange, 400, Map.of("error", validationError));
            return;
        }

        ObjectNode user = userJson(id, request.name(), request.email(), request.accountType());
        users.put(id, user);
        sendJson(exchange, 200, user);
    }

    private void deleteUser(HttpExchange exchange, String path) throws IOException {
        String id = tail(path, "/api/users/");
        ObjectNode removed = users.remove(id);
        if (removed == null) {
            sendJson(exchange, 404, Map.of("error", "User not found"));
            return;
        }
        sendJson(exchange, 204, "");
    }

    private void createTransaction(HttpExchange exchange) throws IOException {
        TransactionRequest request = JsonUtils.mapper().readValue(requestBody(exchange), TransactionRequest.class);
        String validationError = validateTransaction(request);
        if (validationError != null) {
            sendJson(exchange, 400, Map.of("error", validationError));
            return;
        }
        if (!users.containsKey(request.userId())) {
            sendJson(exchange, 404, Map.of("error", "User not found"));
            return;
        }

        String id = "txn-" + transactionSequence.incrementAndGet();
        ObjectNode transaction = JsonUtils.mapper().createObjectNode();
        transaction.put("id", id);
        transaction.put("userId", request.userId());
        transaction.put("amount", request.amount());
        transaction.put("type", request.type());
        transaction.put("recipientId", request.recipientId());
        transaction.put("status", "posted");
        transaction.put("createdAt", Instant.now().toString());
        transactions.put(id, transaction);
        sendJson(exchange, 201, transaction);
    }

    private void getTransactions(HttpExchange exchange, String path) throws IOException {
        String userId = tail(path, "/api/transactions/");
        ArrayNode results = JsonUtils.mapper().createArrayNode();
        transactions.values().stream()
                .filter(transaction -> userId.equals(transaction.get("userId").asText()))
                .forEach(results::add);

        ObjectNode response = JsonUtils.mapper().createObjectNode();
        response.put("userId", userId);
        response.set("transactions", results);
        sendJson(exchange, 200, response);
    }

    private ObjectNode userJson(String id, String name, String email, String accountType) {
        ObjectNode user = JsonUtils.mapper().createObjectNode();
        user.put("id", id);
        user.put("name", name);
        user.put("email", email);
        user.put("accountType", accountType);
        user.put("status", "active");
        user.put("createdAt", Instant.now().toString());
        return user;
    }

    private String validateUser(UserRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            return "name is required";
        }
        if (request.email() == null || request.email().isBlank() || !request.email().contains("@")) {
            return "valid email is required";
        }
        if (request.accountType() == null || !List.of("standard", "premium").contains(request.accountType())) {
            return "accountType must be standard or premium";
        }
        return null;
    }

    private String validateTransaction(TransactionRequest request) {
        if (request.userId() == null || request.userId().isBlank()) {
            return "userId is required";
        }
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            return "amount must be greater than zero";
        }
        if (request.type() == null || !List.of("transfer", "payment", "deposit").contains(request.type())) {
            return "type must be transfer, payment, or deposit";
        }
        if ("transfer".equals(request.type()) && (request.recipientId() == null || request.recipientId().isBlank())) {
            return "recipientId is required for transfers";
        }
        return null;
    }

    private String requestBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private String tail(String path, String prefix) {
        return path.substring(prefix.length());
    }

    private void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        if (status == 204) {
            send(exchange, status, "");
            return;
        }

        String json = body instanceof String value ? value : JsonUtils.mapper().writeValueAsString(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        send(exchange, status, json);
    }

    private void sendHtml(HttpExchange exchange, String html) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        send(exchange, 200, html);
    }

    private void send(HttpExchange exchange, int status, String body) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "authorization, content-type");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        if (status == 204) {
            exchange.sendResponseHeaders(status, -1);
        } else {
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }

    private String uiHtml() {
        return """
                <!doctype html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Fintech Mock Portal</title>
                    <style>
                        body { font-family: Arial, sans-serif; margin: 0; background: #f6f8fa; color: #1f2937; }
                        main { max-width: 920px; margin: 32px auto; padding: 0 20px; display: grid; gap: 24px; }
                        section { background: #fff; border: 1px solid #d8dee4; border-radius: 8px; padding: 20px; }
                        h1, h2 { margin: 0 0 16px; }
                        label { display: grid; gap: 6px; margin: 10px 0; font-size: 14px; font-weight: 600; }
                        input, select { min-height: 36px; border: 1px solid #b7c0cc; border-radius: 6px; padding: 6px 10px; font: inherit; }
                        button { min-height: 38px; padding: 0 14px; border: 0; border-radius: 6px; background: #075985; color: white; font-weight: 700; cursor: pointer; }
                        .message { min-height: 24px; margin-top: 12px; font-weight: 700; }
                        .error { color: #b91c1c; }
                        .success { color: #047857; }
                    </style>
                </head>
                <body>
                <main>
                    <h1>Fintech Mock Portal</h1>
                    <section>
                        <h2>User Registration</h2>
                        <form data-testid="registration-form">
                            <label>Name <input data-testid="name-input" name="name" required></label>
                            <label>Email <input data-testid="email-input" name="email" type="email" required></label>
                            <label>Account Type
                                <select data-testid="account-type-select" name="accountType">
                                    <option value="standard">standard</option>
                                    <option value="premium">premium</option>
                                </select>
                            </label>
                            <button data-testid="register-button" type="submit">Create User</button>
                            <div data-testid="registration-message" class="message"></div>
                        </form>
                    </section>
                    <section>
                        <h2>Create Transaction</h2>
                        <form data-testid="transaction-form">
                            <label>User ID <input data-testid="transaction-user-id-input" name="userId" required></label>
                            <label>Amount <input data-testid="amount-input" name="amount" type="number" min="0.01" step="0.01" required></label>
                            <label>Recipient ID <input data-testid="recipient-id-input" name="recipientId" required></label>
                            <button data-testid="transaction-button" type="submit">Create Transaction</button>
                            <div data-testid="transaction-message" class="message"></div>
                        </form>
                    </section>
                </main>
                <script>
                    const authHeaders = { 'Authorization': 'Bearer %s', 'Content-Type': 'application/json' };
                    const registrationMessage = document.querySelector('[data-testid="registration-message"]');
                    const transactionMessage = document.querySelector('[data-testid="transaction-message"]');

                    document.querySelector('[data-testid="registration-form"]').addEventListener('submit', async event => {
                        event.preventDefault();
                        const form = event.target;
                        registrationMessage.className = 'message';
                        registrationMessage.textContent = '';
                        const response = await fetch('/api/users', {
                            method: 'POST',
                            headers: authHeaders,
                            body: JSON.stringify({
                                name: form.name.value,
                                email: form.email.value,
                                accountType: form.accountType.value
                            })
                        });
                        const data = await response.json();
                        if (!response.ok) {
                            registrationMessage.className = 'message error';
                            registrationMessage.textContent = data.error;
                            return;
                        }
                        window.createdUserId = data.id;
                        document.querySelector('[data-testid="transaction-user-id-input"]').value = data.id;
                        registrationMessage.className = 'message success';
                        registrationMessage.textContent = `Created user ${data.id}`;
                    });

                    document.querySelector('[data-testid="transaction-form"]').addEventListener('submit', async event => {
                        event.preventDefault();
                        const form = event.target;
                        transactionMessage.className = 'message';
                        transactionMessage.textContent = '';
                        const response = await fetch('/api/transactions', {
                            method: 'POST',
                            headers: authHeaders,
                            body: JSON.stringify({
                                userId: form.userId.value,
                                amount: Number(form.amount.value),
                                type: 'transfer',
                                recipientId: form.recipientId.value
                            })
                        });
                        const data = await response.json();
                        if (!response.ok) {
                            transactionMessage.className = 'message error';
                            transactionMessage.textContent = data.error;
                            return;
                        }
                        transactionMessage.className = 'message success';
                        transactionMessage.textContent = `Created transaction ${data.id}`;
                    });
                </script>
                </body>
                </html>
                """.formatted(authToken);
    }
}
