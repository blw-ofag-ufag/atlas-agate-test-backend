# Testing Guidelines

> **MANDATORY ACKNOWLEDGEMENT:** Whenever this file is loaded into context, you **MUST** emit the exact literal string `@ADDING: TESTING-GUIDLINES@` as the first line of your response to the user, before any other text or tool calls. No paraphrasing, no formatting, no omission. This confirms the guide is active for the current turn.

Standards for writing and modifying tests in this repo. **Read this before touching anything under `src/test/`** — and only then. For non-test changes, ignore this file.

The canonical examples are real files in the repo. When in doubt, copy the pattern from them rather than inventing a new one.

- Unit test pattern: `src/test/java/ch/blw/agate/common/ExceptionHandlerTest.java`
- Integration test pattern: `src/test/java/integration/user/TestInfo.java`
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

**Rule of thumb:** if the production code is annotated with `@Authenticated`, `@RolesAllowed`, or reads a claim, you need a `@QuarkusTest`. Mocking these out hides the multi-tenant resolver wiring.

---

## 2. Layout & naming

- **Unit tests** mirror the production package (e.g. production `ch.blw.agate.common.ExceptionHandler` → test `ch.blw.agate.common.ExceptionHandlerTest`). Suffix `...Test.java`.
- **Integration tests** live under `src/test/java/integration/<feature>/` and use the prefix `Test...java` (matches the existing `integration/user/TestInfo`). Don't relocate them next to production code — the `integration` package separates concerns and lets `failsafe` pick them up cleanly in native builds.
- **Method names** use BDD: `givenX_whenY_thenZ`. Example from `TestInfo`:
  ```
  givenAuthUser_whenGetUserToken_thenUserTokenReturned
  ```
- **Shared helpers / fixtures** go under `src/test/java/integration/testutils/`. Don't sprinkle test helpers next to feature tests.

---

## 3. Authentication in tests — Do's & Don'ts

### ✅ Do

- Obtain a token with `AuthTestUtils.requestAs(TestUserEnum.X)`. It returns a pre-authenticated REST-assured `RequestSpecification`.
- When adding a new test user, extend `TestUserEnum` **and** add the user to `src/test/resources/agate-realm.json` (password `secret`) in the same change. Either alone breaks the suite.
- Use role constants from `AuthenticationService` in assertions, not inline strings:
  ```java
  import static ch.blw.agate.common.services.AuthenticationService.AGATE_BENUTZER_ROLE;
  import static ch.blw.agate.common.services.AuthenticationService.AGATE_AGRIDATA_PRODUCER_ROLE;
  ```
  Constants live at `src/main/java/ch/blw/agate/common/services/AuthenticationService.java:24-25`.

### ❌ Don't

- **Don't mock `SecurityIdentity` or `JsonWebToken`.** The Keycloak Dev Service is the source of truth for token shape. Mocking it bypasses the per-request tenant resolver (`resolve-tenants-with-issuer: true`) and lets bugs ship.
- **Don't hand-craft `Authorization: Bearer ...` headers.** Go through `AuthTestUtils`.
- **Don't inline claim names** (`"loginid"`, `"KT_ID_P"`, `"sub"`). Reference them via the test user's getter (`PRODUCER_LUKAS.getAgateLoginId()`) or, in production code, via `AuthenticationService`.
- **Don't try to test brokered-identity (eIAM) flows here.** The test realm doesn't broker. If a flow only makes sense with eIAM, leave a `// not testable without eIAM broker` comment and move on.

### 🟡 Recommended (current gap)

Every new endpoint should add at least:

1. A test that calls without a token and asserts `401`:
   ```java
   RestAssured.given()
       .when().get(MyController.PATH + "/...")
       .then().statusCode(401);
   ```
2. A test that authenticates as a user **without** the required role and asserts `403`.

Existing endpoints don't have these yet — the suite would be stronger if new code added them.

---

## 4. Example: integration test

This is what a well-formed `@QuarkusTest` looks like (abbreviated from `integration/user/TestInfo.java`):

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

Things to copy from this pattern:

- One arrange / act / assert per test, no shared mutable state.
- `.extract().as(new TypeRef<>() {})` for typed extraction — then AssertJ on the typed value.
- Constants for roles, getters for user-specific claim values.

---

## 5. Example: unit test

From `ExceptionHandlerTest.java`. No Quarkus, no DI — instantiate, mock dependencies, assert.

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

Things to copy:

- `@ParameterizedTest` with a `name = "..."` template — prefer it over copy-paste methods that differ only in input.
- Mockito only for collaborators that are awkward to construct (e.g. `ConstraintViolation`). Real objects otherwise.

---

## 6. Assertions

- **HTTP**: REST-assured fluent chain — `given()...when()...then().statusCode(...)`. Use `.then().statusCode(...)` for the status; do not assert it again from the extracted body.
- **Bodies / objects**: **AssertJ only** (`org.assertj.core.api.Assertions.assertThat`).
  - ❌ Don't mix Hamcrest matchers via REST-assured's `.body("field", equalTo(...))`. Pick one style — AssertJ — and keep readability uniform.
- **Typed extraction**: `.extract().as(new TypeRef<>() {})` (for generics) or `.extract().as(MyDto.class)`. Then AssertJ on the typed value.
- **Mocks**: Mockito (`mock(...)`, `when(...).thenReturn(...)`). Use only in pure unit tests, not in `@QuarkusTest`.

---

## 7. Test data

- **Don't share state between tests.** Each test sets up what it needs. Static mutable fields are forbidden.
- **Test users live in `TestUserEnum`.** Extend that enum — don't create parallel enums or per-test inline users.
- **Realm-level data** (roles, clients, identity providers) lives in `src/test/resources/agate-realm.json`. Edits there require running the full suite locally — they affect every test.
- **No builders today.** The suite is small enough that fixture builders would be overkill. If a class needs more than ~3 setup lines repeated across tests, add a small `*Builder` next to the test, not in production code.

---

## 8. Configuration

- Test-only overrides go in `src/main/resources/application-test.yml`. The `test` profile is applied automatically by Quarkus during `./mvnw test`.
- **Avoid `@TestProfile` classes** unless a single test genuinely needs a divergent config. Profile classes proliferate fast and make the suite slow (each unique profile boots a fresh Quarkus instance).
- **Don't reintroduce reactive return types** in tests or test fixtures for controllers — production controllers run on virtual threads (`@RunOnVirtualThread`).
- **Don't move request logging into a `ContainerRequestFilter` "for testability".** `PreSecurityLogFilter` is a Vert.x `@RouteFilter(1500)` on purpose: it runs before OIDC so failed-auth attempts still log. A JAX-RS filter would silently drop those. See CLAUDE.md for the full rationale.

---

## 9. Coverage

- `quarkus-jacoco` collects coverage for `@QuarkusTest`; the regular JaCoCo agent collects coverage for plain unit tests. The split is deliberate — `exclClassLoaders=*QuarkusClassLoader` in `pom.xml` prevents double-counting.
- **Don't "unify" the two agents.** The split exists because the Quarkus classloader is a different beast.
- No hard coverage threshold today. Don't add one without team agreement — a number picked unilaterally will either get ignored or block merges.

---

## 10. Running tests

Mirrors the commands in CLAUDE.md:

```shell
./mvnw test                                                               # full suite (boots Keycloak Dev Service)
./mvnw test -Dtest=TestInfo                                               # one class
./mvnw test -Dtest=TestInfo#givenAuthUser_whenGetUserToken_thenUserTokenReturned   # one method
./mvnw verify -Pnative                                                    # native build + integration tests
./mvnw checkstyle:check                                                   # style (also runs in `verify`)
```

The first test run after a `.m2` purge will be slow — Quarkus downloads the Keycloak Dev Service image. Subsequent runs are fast.

---

## 11. Quick "don't" reference

- ❌ Mock the DB layer (there isn't one — the project is intentionally minimal). If one is added later, integration-test it against a real container, not a mock.
- ❌ Mock the OIDC layer. Use the Keycloak Dev Service.
- ❌ Assert against raw JSON strings. Use typed extraction + AssertJ.
- ❌ Share state between tests via static fields or `@BeforeAll`-mutated members.
- ❌ Inline role strings (`"agate.AgateBenutzer"`) or claim names (`"loginid"`). Use constants and getters.
- ❌ Couple log assertions to a specific log format. If you must assert on logs, target structured fields (MDC keys `requestId`, `userId`) — not the rendered string.
- ❌ Catch and swallow exceptions in a test to "make it green". A failing test is doing its job; fix the production code or delete the test.
- ❌ Use `Thread.sleep(...)` for timing. Use Awaitility or a deterministic synchronization point.
