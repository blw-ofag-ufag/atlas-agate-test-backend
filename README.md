# 🌿 Agate Test Backend

Simulates backends that validate Agate tokens. The service exposes the decoded token claims and user-info for the authenticated user, and serves as a reference implementation for
Agate OIDC integration.

---

## 🚀 Getting Started

### Run in dev mode

```shell
./mvnw quarkus:dev -Dquarkus.profile=local
```

### 🔐 Keycloak

The `local` profile starts the **Keycloak Dev Service**, which serves `src/test/resources/agate-realm.json` (same realm as
`atlas-agate-backend`) on port 6999. No docker-compose setup is needed. The container is shared, so a locally running
`atlas-agate-backend` reuses it.

Tokens from the embedded Keycloak are accepted because `quarkus.oidc.resolve-tenants-with-issuer` maps them onto the
tenant the Dev Service configures. Request the `openid` scope — the app has `user-info-required` enabled, and Keycloak
refuses the UserInfo endpoint for tokens without it.

The SOAP client (`/api/tvd/users`) gets its token from the same Keycloak (client `tvd-agate-api`) and calls the locally
running `atlas-agate-backend` (`http://localhost:8901/backend/soap/AdminService/v1_44`); override with `SOAP_ENDPOINT_URL`.

### 🧪 HTTP requests

`http/` contains IntelliJ HTTP client requests. The `local` environment in `http/env/http-client.env.json` uses the realm
users `biosuisse`, `erika`, `av` (password grant) and the `tvd` client (client credentials).

### 🔎 Tracing

`quarkus-opentelemetry` continues an inbound W3C `traceparent` header (or starts a new trace) and puts `traceId`/`spanId`
into the MDC; both are part of the log format and the `traceId` is returned in error responses (`ExceptionDto`).
Traces are not exported yet (`quarkus.otel.traces.exporter=none`).

---

## 🧪 Tests

`@QuarkusTest` spins up a Keycloak container configured similarly to the real Agate realm, but without brokering authentication to eIAM. Tests can also be driven via Swagger UI or
the `.http` test files.

> **Heads-up when refreshing `src/test/resources/agate-realm.json` from `atlas-agate-local`:**
> set `"directAccessGrantsEnabled" : true` on the `agridata` client. The Quarkus integration
> tests use the OAuth password grant to obtain tokens; without this flag Keycloak rejects
> the request with an HTML error page.

---

## ⚙️ GitHub Workflow

Uses the BLW shared workflow: [atlas-code-github-workflows](https://github.com/blw-ofag-ufag/atlas-code-github-workflows)

---

## ✅ Checkstyle

Import the Checkstyle plugin in your IDE and point it at `checkstyle.xml` in the repository root to catch violations before pushing.

In the IDE Checkstyle settings, exclude SQL scripts:

```
*.{sql}
```

Run Checkstyle:

```shell
./mvnw checkstyle:check
```

---

## 🔖 Semantic Versioning

Versioning is driven by two files:

| File              | Purpose                                                  |
|-------------------|----------------------------------------------------------|
| `package.json`    | Pins the `pnpm` version used to run semantic-release     |
| `.releaserc.json` | Configures branches, version strategy, and release steps |

---

## 🎨 ASCII Banner

Generated with [manytools.org ASCII banner](https://manytools.org/hacker-tools/ascii-banner/) — font: **Slant**