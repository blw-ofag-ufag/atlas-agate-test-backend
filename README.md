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
| 🖥️ Local Keycloak   | Spin up via [atlas-agate-local](https://github.com/blw-ofag-ufag/atlas-agate-local) (shared instance on the fixed port 9999) |
| 📦 Embedded Keycloak | Copy the OIDC config from `application-test.yml` into your local profile            |

The Keycloak Dev Service used by the tests runs the **custom Agate Keycloak image delivered to ECR**,
pinned to a fixed version — not a stock Keycloak. It carries the Agate custom providers (e.g. the
`oidc-filtered-claim-mapper`), so the test token matches the one the `dev` environment issues. The
image lives in a **private** ECR repository, so you must authenticate once before the first test run
(and whenever your SSO session has expired):

```shell
# 1. Log in to AWS SSO (profile atlas-dev → dev account 681832766879, eu-central-2)
aws sso login --profile atlas-dev

# 2. Docker-login to the ECR registry that holds the image
aws ecr get-login-password --region eu-central-2 --profile atlas-dev \
  | docker login --username AWS --password-stdin 681832766879.dkr.ecr.eu-central-2.amazonaws.com
```

The exact tag is set in `application-test.yml` (`quarkus.keycloak.devservices.image-name`); it is
shared with `atlas-agate-backend` and the local port is 9999. In CI the image is pulled via a
dedicated ECR pull role (ADR-1005); see the GitHub Workflow section.

---

## 🧪 Tests

`@QuarkusTest` spins up the custom Agate Keycloak container (the pinned ECR image, see 🔐 Keycloak)
configured similarly to the real Agate realm, but without brokering authentication to eIAM. Tests can
also be driven via Swagger UI or the `.http` test files.

> **Heads-up when refreshing `src/test/resources/agate-realm.json`:** it is a _manual_ export from
> [atlas-agate-local](https://github.com/blw-ofag-ufag/atlas-agate-local) — re-run its
> `scripts/regenerate-realm-export.sh` and copy the file here, do not hand-edit it. The script keeps
> `"directAccessGrantsEnabled" : true` on the `agridata` and `agate` clients (the Quarkus integration
> tests use the OAuth password grant to obtain tokens; without it Keycloak rejects the request with an
> HTML error page). This manual export is temporary and will be replaced by the versioned realm
> artifact (EK-1006 / N2).

---

## ⚙️ GitHub Workflow

Uses the BLW shared workflow: [atlas-code-github-workflows](https://github.com/blw-ofag-ufag/atlas-code-github-workflows)

The test job pulls the custom Keycloak image from the private ECR repo via a dedicated **GitHub OIDC
pull role, assumed directly without role chaining** (ADR-1005 — a deliberate exception so that
`pull_request` runs, which have no GitHub environment, can still pull the image). The role is defined
in `pc-core-blw-agate-dev` (`keycloak-ecr-pull-role.tf`, output `keycloak_ecr_pull_role_arn`); its ARN
is a non-secret workflow input passed as the repo/org variable `KEYCLOAK_ECR_PULL_ROLE_ARN` to the
central workflow's `keycloak-ecr-pull-role-arn` input.

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