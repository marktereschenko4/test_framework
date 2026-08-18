# Fintech Test Automation Framework

Java test automation framework using Playwright for both API and UI coverage against a mock fintech microservices surface.

## What Is Included

- API tests for users and transactions
- UI tests against an in-process mock frontend
- Test data factories for unique users and transactions
- Environment profiles for local and QA-style execution
- Custom API assertions and helper client
- API response logging under `target/api-responses`
- JUnit/Surefire reports under `target/surefire-reports`
- UI failure screenshots under `target/playwright-screenshots`

## Tech Stack

- Java 17+
- Maven
- JUnit 5
- Playwright Java
- Jackson
- JDK `HttpServer` mock service

## Run Locally

Install Playwright browser binaries and other libraries using maven:

```bash
mvn clean install -DskipTests"
```

Run all tests against the self-contained mock app:

```bash
mvn test
```

Run only API tests:

```bash
mvn -Dtest='*ApiTest' test
```

Run only UI tests:

```bash
mvn -Dtest='*UiTest' test
```

Switch environment:

```bash
mvn -Pqa test
```

For `qa`, set the token referenced by `src/test/resources/environments/qa.properties`:

```bash
export QA_API_TOKEN='replace-me'
```

## Configuration

Environment files live in `src/test/resources/environments`.

- `local.properties` starts the in-process mock app on a dynamic port.
- `qa.properties` shows how to target an external environment.
- Maven properties control browser selection and headless mode.

Example:

```bash
mvn test -Dbrowser=chromium -Dheadless=false
```

## Mock API Contract

Required endpoints:

- `POST /api/users`
- `GET /api/users/:id`
- `POST /api/transactions`
- `GET /api/transactions/:userId`

The mock app also supports `PUT /api/users/:id` and `DELETE /api/users/:id` so the API suite can demonstrate full CRUD coverage.

All API endpoints require:

```text
Authorization: Bearer test-token
```

## Project Layout

```text
src/test/java/com/fintech/framework/api          API client helper
src/test/java/com/fintech/framework/assertions   Custom assertions
src/test/java/com/fintech/framework/base         API/UI base tests
src/test/java/com/fintech/framework/config       Environment loading
src/test/java/com/fintech/framework/data         Test data factories
src/test/java/com/fintech/framework/extensions   Reporting extensions
src/test/java/com/fintech/framework/mock         Mock API and UI app
src/test/java/com/fintech/framework/tests        API and UI test suites
src/test/java/com/fintech/framework/utils        JSON and artifact utilities
```
