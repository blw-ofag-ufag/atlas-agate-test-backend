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

**Registry access.** The image lives in the private ECR of the Agate dev account (681832766879). Let the
[ECR credential helper](https://github.com/awslabs/amazon-ecr-credential-helper) fetch the token through your AWS profile
whenever Docker or Testcontainers pulls — no `docker login`:

1. Docker Desktop for macOS ships `docker-credential-ecr-login`; elsewhere download it from the releases of that
   repository and put it on your `PATH`. Check with `docker-credential-ecr-login version`.
2. Route the registry to it in `~/.docker/config.json` (Windows: `%USERPROFILE%\.docker\config.json`), keeping the
   existing content:

   ```json
   "credHelpers": {
     "681832766879.dkr.ecr.eu-central-2.amazonaws.com": "ecr-login"
   }
   ```

   Remove a token from an earlier login: `docker logout 681832766879.dkr.ecr.eu-central-2.amazonaws.com`.
3. Make the dev account your default profile — the helper takes it from `AWS_PROFILE`, also when Testcontainers calls
   it from the JVM. `atlas-dev` is the profile set up in
   [pc-core-blw-agate-dev](https://github.com/blw-ofag-ufag/pc-core-blw-agate-dev); use yours if it differs.

   ```shell
   echo 'export AWS_PROFILE=atlas-dev' >> ~/.zshrc   # Windows: setx AWS_PROFILE atlas-dev
   ```

   Open a new terminal afterwards (and restart your IDE if you start the app from there). Other accounts (int, prod,
   bbs …) always with an explicit `--profile`, which overrides the default.
4. When the SSO session has expired (every 12 h): `aws sso login`.

`no basic auth credentials` means `AWS_PROFILE` is missing in that process or the SSO session has expired — a token
cached in `~/.ecr/cache.json` does not help without the profile. Details are in `~/.ecr/log/ecr-login.log`.

**Alternative without the helper: `docker login` per session.** The previous way still works — skip steps 1–3 and log
Docker in yourself once per session (the token lasts 12 h). On Windows the Credential Manager may reject this token as
too long; use the helper there.

```shell
aws sso login --profile atlas-dev
aws ecr get-login-password --region eu-central-2 --profile atlas-dev \
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

`@QuarkusTest` spins up the delivered Keycloak image (see [Keycloak](#-keycloak); ECR login required)
with the Agate realm export, without brokering authentication to eIAM. Tests can also be driven via
Swagger UI or the `.http` test files.

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