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

By default the app connects to the Keycloak instance on the `dev` environment. Two alternatives:

| Option               | How                                                                                 |
|----------------------|-------------------------------------------------------------------------------------|
| 🖥️ Local Keycloak   | Spin up via [atlas-agate-local](https://github.com/blw-ofag-ufag/atlas-agate-local) |
| 📦 Embedded Keycloak | Copy the OIDC config from `application-test.yml` into your local profile            |

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