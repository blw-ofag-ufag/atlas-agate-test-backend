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
`atlas-agate-backend`) on port 9999. No docker-compose setup is needed. The container is shared with
`atlas-agate-backend`: whichever backend starts first creates it, the other reuses it — so only one Keycloak may run on 9999.

The Dev Service (profiles `local` and `test`) runs the **delivered Agate Keycloak image** from ECR, pinned in
`application-local.yml` and `application-test.yml` (`quarkus.keycloak.devservices.image-name`, same pin as
`atlas-agate-backend`), with `realm-name: agate`. Its custom mappers are active, so tokens look like on dev.

The Keycloak of atlas-agate-local binds 9999 too and is not reused. Stop it (`docker compose stop keycloak` there)
before `quarkus:dev`, or run against it instead of the Dev Service: `-Dquarkus.keycloak.devservices.enabled=false`.

**Registry access (once per session).** The image lives in the private ECR of the Agate dev account, so log in before
the first `quarkus:dev` or `./mvnw verify`:

```shell
aws sso login --profile <profile-of-the-agate-dev-account>   # account 681832766879
aws ecr get-login-password --region eu-central-2 --profile <profile-of-the-agate-dev-account> \
  | docker login --username AWS --password-stdin 681832766879.dkr.ecr.eu-central-2.amazonaws.com
```

CI pulls the image through its own pull role (`agate-ecr-pull-role`) — nothing to do there.

**Realm.** `src/test/resources/agate-realm.json` is a manual export from
[atlas-agate-local](https://github.com/blw-ofag-ufag/atlas-agate-local) (`scripts/regenerate-realm-export.sh`, including
its marked `local-test-additions.json`), copied byte-identical into both backends — never edit it here. It is temporary:
the versioned realm artifact replaces it (A1A-602). Bump the image pin (both profile files) together with the export.

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