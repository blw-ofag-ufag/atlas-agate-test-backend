---
name: testing-guidelines
description: Use when writing, modifying, reviewing, or auditing tests under src/test/ in this repo. Enforces choice of test type (plain JUnit + Mockito vs. @QuarkusTest + REST-assured + Keycloak Dev Service), package/naming layout, AssertJ-only assertions, role/claim constants from AuthenticationService, and the do's/don'ts around mocking OIDC, SecurityIdentity, or JsonWebToken. TRIGGER when the user asks to "add tests", "write a test", "test this", "review tests", "improve test coverage" for any file under src/main/ — or when generating files whose path lies under src/test/. SKIP for non-test changes.
version: 1.0.0
user-invocable: true
allowed-tools:
  - Read
  - Edit
  - Write
  - Bash(./mvnw test*)
  - Bash(./mvnw checkstyle:check)
  - Bash(./mvnw verify*)
  - Bash(find src/test *)
  - Bash(grep *)
---

# Testing Guidelines Skill

Authoritative standards for tests in this repo. Apply these rules whenever touching `src/test/` — do not apply them to non-test code.

The canonical examples live in the repo. When unsure, copy the pattern from them rather than inventing a new one:

- Unit test pattern: `src/test/java/ch/blw/agate/common/ExceptionHandlerTest.java`
- Integration test pattern: `src/test/java/integration/user/TestInfo.java`
- Filter unit-test pattern (Vert.x mocks + JBoss LogManager level config): `src/test/java/ch/blw/agate/common/filters/PreSecurityLogFilterTest.java`
- Auth helper: `src/test/java/integration/testutils/AuthTestUtils.java`
- Test users: `src/test/java/integration/testutils/TestUserEnum.java`
- Test realm: `src/test/resources/agate-realm.json`

---

## 1. Choosing the right test type

| Scenario | Use | Why |
|---|---|---|
| Pure logic, no CDI, no HTTP | Plain JUnit (no `@QuarkusTest`) | Fast — no container boot. See `ExceptionHandlerTest`. |
| Endpoint, OIDC, CDI wiring, or anything that depends on Quarkus runtime | `@QuarkusTest` + REST-assured | Exercises the real OIDC chain via the embedded Keycloak Dev Service. See `TestInfo`. |
| Negative auth paths (401 without token, 403 wrong role, expired/malformed JWT) | `@QuarkusTest` **without** `AuthTestUtils.requestAs(...)` | Currently a gap. Recommended for every new endpoint. |
| Vert.x `@RouteFilter` or other pre-security plumbing | Plain JUnit + Mockito on `RoutingContext` | Filters are package-private — colocate the test in the same package. See `PreSecurityLogFilterTest`. |

**Rule of thumb:** if the production code is annotated with `@Authenticated`, `@RolesAllowed`, or reads a claim, you need a `@QuarkusTest`. Mocking these out hides the multi-tenant resolver wiring.

---

## 2. Layout & naming

- **Unit tests** mirror the production package (e.g. `ch.blw.agate.common.ExceptionHandler` → `ch.blw.agate.common.ExceptionHandlerTest`). Suffix `...Test.java`.
- **Integration tests** live under `src/test/java/integration/<feature>/` and use the prefix `Test...java`. Don't relocate them next to production code — the `integration` package separates concerns and lets failsafe pick them up cleanly in native builds.
- **Method names** use BDD: `givenX_whenY_thenZ`. Example: `givenAuthUser_whenGetUserToken_thenUserTokenReturned`.
- **Shared helpers / fixtures** go under `src/test/java/integration/testutils/`. Don't sprinkle test helpers next to feature tests.

---

## 3. Authentication in tests — Do's & Don'ts

### Do

- Obtain a token with `AuthTestUtils.requestAs(TestUserEnum.X)`. It returns a pre-authenticated REST-assured `RequestSpecification`.
- When adding a new test user, extend `TestUserEnum` **and** add the user to `src/test/resources/agate-realm.json` (password `secret`) in the same change.
- Use role constants from `AuthenticationService` in assertions, not inline strings:
  ```java
  import static ch.blw.agate.common.services.AuthenticationService.AGATE_BENUTZER_ROLE;
  import static ch.blw.agate.common.services.AuthenticationService.AGATE_AGRIDATA_PRODUCER_ROLE;
  ```

### Don't

- **Don't mock `SecurityIdentity` or `JsonWebToken`.** The Keycloak Dev Service is the source of truth. Mocking bypasses the per-request tenant resolver (`resolve-tenants-with-issuer: true`).
- **Don't hand-craft `Authorization: Bearer ...` headers.** Go through `AuthTestUtils`.
- **Don't inline claim names** (`"loginid"`, `"KT_ID_P"`, `"sub"`). Reference them via the test user's getter (e.g. `PRODUCER_LUKAS.getAgateLoginId()`) or, in production code, via `AuthenticationService`.
- **Don't try to test brokered-identity (eIAM) flows here.** The test realm doesn't broker. Leave a `// not testable without eIAM broker` comment and move on.

### Recommended (current gap)

Every new endpoint should add at least:

1. A test that calls without a token and asserts `401`:
   ```java
   RestAssured.given().when().get(MyController.PATH + "/...").then().statusCode(401);
   ```
2. A test that authenticates as a user **without** the required role and asserts `403`.

---

## 4. Example: integration test

```java
@QuarkusTest
public class TestInfo {

  @Test
  void givenAuthUser_whenGetUserToken_thenUserTokenReturned() {
    Map<String, Object> userToken = AuthTestUtils.requestAs(PRODUCER_LUKAS)
        .when().get(UserController.PATH + "/user-token")
        .then().statusCode(200)
        .extract().as(new TypeRef<>() {});

    assertThat(userToken).containsEntry("sub", PRODUCER_LUKAS.getSub());
    assertThat(userToken).containsEntry("loginid", PRODUCER_LUKAS.getAgateLoginId());

    var roles = (List<Map<String, Object>>)
        ((Map<String, Object>) userToken.get("realm_access")).get("roles");
    assertThat(roles).extracting(r -> r.get("string"))
        .containsExactlyInAnyOrder(AGATE_AGRIDATA_PRODUCER_ROLE, AGATE_BENUTZER_ROLE);
  }
}
```

Copy from this pattern: one arrange/act/assert per test; `.extract().as(new TypeRef<>() {})` for typed extraction; constants for roles; getters for user-specific claim values.

---

## 5. Example: unit test

```java
class ExceptionHandlerTest {

  private ExceptionHandler exceptionHandler;

  @BeforeEach
  void setUp() {
    exceptionHandler = new ExceptionHandler();
    MDC.put(REQUEST_ID_MDC_FIELD, "test-request-id");
  }

  @ParameterizedTest(name = "handleAll, debug={0}")
  @ValueSource(booleans = {false, true})
  void handleAll(boolean debug) {
    exceptionHandler.returnDebug = debug;
    Response response = exceptionHandler.handleAll(new RuntimeException("boom"));
    ExceptionDto dto = (ExceptionDto) response.getEntity();

    assertThat(response.getStatus()).isEqualTo(500);
    assertThat(dto.message()).isEqualTo("An error occurred");
    assertThat(dto.requestId()).isEqualTo("test-request-id");
    assertThat(dto.debugMessage()).isEqualTo(debug ? "boom" : null);
  }
}
```

Copy from this pattern: `@ParameterizedTest(name = "...")` over copy-pasted methods that differ only in input; Mockito only for collaborators that are awkward to construct.

---

## 6. Example: Vert.x route-filter unit test

For `@RouteFilter` classes, mock `RoutingContext` / `HttpServerRequest` / `HttpServerResponse`, set `java.util.logging.Logger` levels to drive the log-gated branches, and live in the same package so the filter's package-private methods are reachable. See `PreSecurityLogFilterTest` for the working template. Notes:

- Surefire already sets `java.util.logging.manager=org.jboss.logmanager.LogManager`, so `Logger.getLogger(...).setLevel(Level.INFO|WARNING|FINEST)` will toggle `log.isInfoEnabled()` / `log.isTraceEnabled()` for SLF4J calls in the production code.
- Setting `Level.FINE` to enable SLF4J `isDebugEnabled()` via the jboss-logmanager binding is unreliable — prefer covering the DEBUG branch in an integration test with `quarkus.log.level=DEBUG` rather than fighting the log backend.
- Always restore the original logger level in `@AfterEach` to avoid bleeding state into other tests.
- Verify observable Vert.x interactions (`ctx.next()`, `ctx.addBodyEndHandler(...)`, `request.bodyHandler(...)`) rather than the rendered log string.

---

## 7. Assertions

- **HTTP**: REST-assured fluent chain — `given()...when()...then().statusCode(...)`. Don't assert the status again from the extracted body.
- **Bodies / objects**: **AssertJ only** (`org.assertj.core.api.Assertions.assertThat`).
- Don't mix Hamcrest matchers via REST-assured's `.body("field", equalTo(...))`. Pick AssertJ and keep readability uniform.
- **Typed extraction**: `.extract().as(new TypeRef<>() {})` for generics or `.extract().as(MyDto.class)`. Then AssertJ on the typed value.
- **Mocks**: Mockito (`mock(...)`, `when(...).thenReturn(...)`). Use only in pure unit tests, not in `@QuarkusTest`.

---

## 8. Test data

- **Don't share state between tests.** Each test sets up what it needs. Static mutable fields are forbidden.
- **Test users live in `TestUserEnum`.** Extend that enum — don't create parallel enums or per-test inline users.
- **Realm-level data** (roles, clients, identity providers) lives in `src/test/resources/agate-realm.json`. Edits there require running the full suite locally — they affect every test.
- **No builders today.** If a class needs more than ~3 setup lines repeated across tests, add a small `*Builder` next to the test, not in production code.

---

## 9. Configuration

- Test-only overrides go in `src/main/resources/application-test.yml`. The `test` profile is applied automatically by Quarkus during `./mvnw test`.
- **Avoid `@TestProfile` classes** unless a single test genuinely needs a divergent config. Each unique profile boots a fresh Quarkus instance.
- **Don't reintroduce reactive return types** in tests for controllers — production controllers run on virtual threads (`@RunOnVirtualThread`).
- **Don't move request logging into a `ContainerRequestFilter` "for testability".** `PreSecurityLogFilter` is a Vert.x `@RouteFilter(1500)` on purpose: it runs before OIDC so failed-auth attempts still log. See CLAUDE.md for the full rationale.

---

## 10. Coverage

- `quarkus-jacoco` collects coverage for `@QuarkusTest`; the regular JaCoCo agent collects coverage for plain unit tests. The split is deliberate (`exclClassLoaders=*QuarkusClassLoader` in `pom.xml` prevents double-counting).
- **Don't "unify" the two agents.**
- No hard coverage threshold today. Don't add one without team agreement.

---

## 11. Running tests

```shell
./mvnw test                                                                          # full suite (boots Keycloak Dev Service)
./mvnw test -Dtest=TestInfo                                                          # one class
./mvnw test -Dtest=TestInfo#givenAuthUser_whenGetUserToken_thenUserTokenReturned     # one method
./mvnw verify -Pnative                                                               # native build + integration tests
./mvnw checkstyle:check                                                              # style (also runs in `verify`)
```

The first run after a `.m2` purge will be slow — Quarkus downloads the Keycloak Dev Service image.

---

## 12. Quick "don't" reference

- Don't mock the DB layer (there isn't one). If one is added later, integration-test it against a real container.
- Don't mock the OIDC layer. Use the Keycloak Dev Service.
- Don't assert against raw JSON strings. Use typed extraction + AssertJ.
- Don't share state between tests via static fields or `@BeforeAll`-mutated members.
- Don't inline role strings (`"agate.AgateBenutzer"`) or claim names (`"loginid"`). Use constants and getters.
- Don't couple log assertions to a specific log format. If you must assert on logs, target structured MDC fields (`requestId`, `userId`) — not the rendered string.
- Don't catch and swallow exceptions to "make a test green". Fix the production code or delete the test.
- Don't use `Thread.sleep(...)` for timing. Use Awaitility or a deterministic synchronization point.

---

## When this skill is invoked

1. Re-read the canonical example closest to the task before generating code.
2. Choose the test type per §1 — do not default to `@QuarkusTest` when plain JUnit suffices.
3. Apply the do's/don'ts in §3 even when the user's prompt is silent on auth.
4. Run `./mvnw test -Dtest=<NewTestClass>` and `./mvnw checkstyle:check` before reporting the task complete.
