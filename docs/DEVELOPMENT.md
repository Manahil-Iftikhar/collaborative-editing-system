# Local development

## Setup

Install JDK 17 and Maven 3.9+, then verify `java -version` and `mvn -version`.

```bash
git clone https://github.com/Manahil-Iftikhar/collaborative-editing-system.git
cd collaborative-editing-system
mvn -f user-service/pom.xml clean verify
mvn -f document-service/pom.xml clean verify
mvn -f version-service/pom.xml clean verify
mvn -f api-gateway/pom.xml clean verify
```

There is no root aggregator POM or Maven wrapper. Use the module-specific commands. All four module verification jobs passed in [GitHub Actions run 36335878210](https://github.com/Manahil-Iftikhar/collaborative-editing-system/actions/runs/36335878210) on September 27, 2026 at source commit `5275633a709bffa76c4479ece6c53d31eaf3cd38`.

Set `JWT_SECRET` in the user-service terminal as described below. Start all four modules in separate terminals using the commands in the [README](../README.md). Keep those terminals open. Start the user service first, then the document service, version service, and gateway. Wait for each service to finish starting before trying the editor.

| Variable | Set in | Local default / requirement |
| --- | --- | --- |
| `JWT_SECRET` | User-service terminal | Required; generate as described below |
| `USER_SERVICE_URL` | Document- and version-service terminals | `http://localhost:8081` |
| `DOCUMENT_SERVICE_URL` | Version-service terminal | `http://localhost:8082` |

The service URLs point directly to trusted backend services. Leave their defaults for the standard local setup; change them only when the corresponding backend address changes.

## Browser walkthrough

1. Open the local `collab-editor.html` file.
2. Register a sample account and log in.
3. Create a sample document.
4. Edit and save its content.
5. Create a version snapshot using the separate version action.
6. Inspect version history and load an earlier snapshot.

Loading a version into the editor is not equivalent to a cross-service rollback. Check the current document and saved snapshot independently.

The smaller `test-ui.html` is also included. Neither UI provides proven simultaneous editing.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| Connection refused | All four modules must be running; confirm ports 8080–8083 are free |
| Browser fetch/CORS error | Inspect browser console and gateway logs; UI targets port 8080 |
| Empty data after restart | H2 databases are in memory and use `create-drop` |
| Changes missing from version history | Editing a document and creating a snapshot are separate calls |
| 401 on a protected API | Log in again and send `Authorization: Bearer <token>`; a changed signing secret invalidates old tokens |
| 403 on document/version operations | Use the document owner's account; public visibility does not grant history or write access |
| 503 on a protected document/version API | Check user/document service logs and configured upstream URLs; verification failures deny access |
| User service fails at startup | Set a fresh `JWT_SECRET` of at least 32 UTF-8 bytes in that terminal |
| Maven failure | Confirm JDK selection, dependency access, and the module POM used |

If a browser restricts requests from local files, serve the repository through a local static server and open `collab-editor.html` there. Do not expose the development services publicly.

## Tests

```bash
mvn -f user-service/pom.xml test
mvn -f document-service/pom.xml test
mvn -f version-service/pom.xml test
```

Read the generated `target/surefire-reports/` within each service for actual execution results. There are 67 declared test methods: 39 service tests, five JWT utility tests, six user-access tests, nine document-access/identity-client tests, and eight version-access/ownership-client tests. See the [README test table](../README.md#tests-and-verification) for the per-class breakdown. No gateway test class is present in the reviewed tree.

## Review scope

The initial review covered the original prototype at source commit `9842a086550e18fec3934a5105b9491594462113`. This guide now includes the JWT and owner-access changes through source commit `5275633a709bffa76c4479ece6c53d31eaf3cd38`, whose Java 17 CI run is linked above. The browser walkthrough is a manual procedure, not a claim that an end-to-end browser test has passed.

## Automated checks

The [Java checks workflow](../.github/workflows/java-checks.yml) verifies each module independently on pushes and pull requests. Available Surefire reports are uploaded even when a job fails and retained for 14 days. The gateway has build coverage only; focused authorization tests are included, but passing these jobs does not verify the full browser/gateway/service chain or simultaneous editing.

## JWT configuration

Before starting the user service, set `JWT_SECRET` to a fresh, cryptographically random value. The value must contain at least 32 UTF-8 bytes; length validation is not an entropy guarantee. No default is supplied.

If OpenSSL is installed, bash users can set it without printing it:

```bash
export JWT_SECRET="$(openssl rand -hex 32)"
```

PowerShell users can generate a value using .NET:

```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$rng.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
```

Run these in the same terminal used to launch the user service. Keep the value private and stable for the intended token lifetime; changing it invalidates previously signed tokens. Do not commit or share it.

Tests inject explicitly test-only signing material; it is never a runtime fallback. Five JWT tests exercise configuration rejection, subject matching, malformed input, a different signing key, and expiry.

The previous signing value remains in Git history. If it was used in a deployment, replace it in that environment. User-profile requests now enforce bearer authentication and account ownership. Document and version APIs now also enforce owner permissions.

## User-service access checks

The six MockMvc tests use real registration/login, database fixtures, and the security filter chain. They cover anonymous reads/writes, owner access, cross-account denial, invalid signatures, unknown/inactive users, and denied unlisted routes. The editor includes the token in profile requests. H2 browser consoles are disabled by default in all three backend services. User-service routes also remain protected by the default-deny policy. The integration smoke check verifies that console URLs cannot be opened directly on the backend ports.

## Document authorization

Protected document requests validate the bearer token through `GET /api/users/me` at `USER_SERVICE_URL` (default `http://localhost:8081`). Start the user service before using private document reads or writes. Calls have three-second connection/read timeouts and deny access when verification fails. Configure only a trusted identity-service URL; use a protected transport/network in any future deployment.

The editor now sends its token on document calls. The older `test-ui.html` does not attach tokens and cannot perform protected document writes; use the main editor instead.

Nine tests exercise document owner rules and the identity HTTP client. HTTP identity responses are mocked in these focused tests; the real HTTP smoke check below complements these tests; browser testing remains future work. Version APIs now also enforce document-owner authorization, as described below.

## Version authorization

Every version API call needs a bearer token. The service validates the active account through `USER_SERVICE_URL` (default `http://localhost:8081`) and ownership through `DOCUMENT_SERVICE_URL` (default `http://localhost:8082`). Calls use three-second connection/read timeouts and deny access if authorization cannot be confirmed.

Public visibility applies only to current document content, not historical snapshots or contributions. Revert creates a snapshot; it does not modify current document content. The main editor sends tokens on version calls; the older test UI does not support protected workflows.

Eight added tests cover all five endpoint policies, verified actor IDs, public-document history denial, missing identity, upstream errors, and malformed responses. There are 67 declared tests. Service-to-service responses are mocked in focused tests; the real gateway smoke check below complements these tests. Browser interaction and production configuration remain outside its scope.

## Gateway integration smoke check

The [integration workflow](../.github/workflows/integration-smoke.yml) packages and starts all four real services, then runs [scripts/integration_smoke.py](../scripts/integration_smoke.py) through the gateway. No identity or document HTTP response is mocked. It covers registration/login, private/public document access, owner-only editing and version APIs, ignored forged actor IDs, snapshot-only revert behavior, and denial during an identity-service outage.

To reproduce with Java 17, Maven and Python 3.10+ installed, run from the repository root in bash:

```bash
for module in user-service document-service version-service api-gateway; do
  mvn --batch-mode --no-transfer-progress -f "$module/pom.xml" -DskipTests package
done
python3 scripts/integration_smoke.py
```

Stop existing services first: ports 8080–8083 must be free. The script refuses occupied ports, generates an ephemeral signing secret, creates disposable accounts/documents in fresh in-memory databases, and stops only the processes it started. Results and service logs go to `integration-results/`; CI retains them for 14 days. It deliberately stops its user service at the end to test authorization failure. The separate Java checks workflow still runs the 67 declared Java tests; packaging here skips those duplicate tests.

This checks sequential HTTP interactions, not browser rendering/CORS, concurrent editing, deployment security, or every possible upstream failure. Consult the workflow run result for whether a particular revision passed.
