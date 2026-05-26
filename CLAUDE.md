# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Purpose

Quarkus 3 / Java 25 reference backend that validates Agate OIDC tokens. Endpoints under `/test-backend/api/user/v1` expose decoded JWT claims, OIDC userinfo, and a role-gated dummy resource. It is intentionally minimal — no datastore, no business logic — its job is to exercise Agate (Keycloak) integration.

## Common commands

```shell
# Dev mode against the dev-env Keycloak (default)
./mvnw quarkus:dev -Dquarkus.profile=local

# Run all tests (spins up a Keycloak Dev Service using src/test/resources/agate-realm.json)
./mvnw test

# Run a single test class / method
./mvnw test -Dtest=TestInfo
./mvnw test -Dtest=TestInfo#givenAuthUser_whenGetUserToken_thenUserTokenReturned

# Checkstyle (also runs in the `verify` phase)
./mvnw checkstyle:check

# Native build + integration tests
./mvnw verify -Pnative
```

Swagger UI: `http://localhost:8900/test-backend/q/swagger-ui` once running. `.http` files under `http/` are runnable from IntelliJ for ad-hoc calls.

## Architecture notes worth knowing up front

- **Multi-tenant OIDC.** `application-local.yml` defines two named tenants (`agate-new`, `agate-old`) plus a default `quarkus.oidc` block, and resolves which one to use per request via `resolve-tenants-with-issuer: true`. When touching OIDC config, update all tenant blocks consistently — a token from `agate-old` will fail if only the default tenant is configured.
- **Auth is enforced declaratively in `application.yml`**, not via annotations alone. The `quarkus.http.auth.permission` map protects everything under `api/*` (authenticated), exempts `OPTIONS` (CORS preflight), and leaves `q/*` (health, metrics, swagger) public. `@Authenticated` / `@RolesAllowed` on controllers is a second layer; both must agree.
- **Pre-security request logging is unusual on purpose.** `PreSecurityLogFilter` is a Vert.x `@RouteFilter(1500)` (hence the `quarkus-reactive-routes` dependency, see the TODO in `pom.xml`). It runs **before** the OIDC mechanism so failed auth attempts still produce structured logs with `requestId` / `userId` MDC keys populated by `PreSecurityMdcFilter`. Don't move this logic into a JAX-RS `ContainerRequestFilter` — that runs after authentication and would drop unauthenticated requests from the log.
- **Claims access goes through `AuthenticationService`**, not directly through `SecurityIdentity` / `JsonWebToken`, so claim names (`loginid`, `KT_ID_P`, `sub`, role constants) stay in one place. Add new claim accessors here rather than in controllers.
- **Roles are Agate-specific strings**, defined as constants on `AuthenticationService` (`agate.Agridata_Einwilliger`, `agate.AgateBenutzer`). Use the constants in `@RolesAllowed` — don't inline the strings.

## Tests

- `@QuarkusTest` uses Quarkus's Keycloak Dev Service, seeded from `src/test/resources/agate-realm.json`. Test users and their expected claims live in `integration/testutils/TestUserEnum.java`; auth flow goes through `AuthTestUtils.requestAs(user)`.
- The test Keycloak mirrors the real Agate realm's structure but does not broker to eIAM, so brokered-identity flows can't be tested here.
- `quarkus-jacoco` collects coverage for `@QuarkusTest` (the regular JaCoCo agent is configured with `exclClassLoaders=*QuarkusClassLoader` to avoid double-counting).
- **When writing or modifying tests, read `.claude/testing-guidelines.md` first and follow its do's / don'ts. Do not load it for non-test changes.**

## Code style

- Checkstyle config: `checkstyle.xml` (repo root). Bound to the `verify` phase with `violationSeverity=warning`. CI fails on violations.
- Lombok + MapStruct annotation processors are wired in `pom.xml` — keep both paths in `annotationProcessorPaths` when adding processors.
- Controllers run on virtual threads (`@RunOnVirtualThread`). Don't reintroduce reactive return types in the user-facing API.

## Release / CI

- Reusable workflow: `blw-ofag-ufag/atlas-code-github-workflows/.github/workflows/backend_workflow.yml`. Branch → env mapping: `develop` → `dev`, `main` → `int`.
- Versioning is semantic-release driven; `package.json` pins the pnpm version and `.releaserc.json` configures branches and release steps. Don't bump `<version>` in `pom.xml` by hand — release commits do it.