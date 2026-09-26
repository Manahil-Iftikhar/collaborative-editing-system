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

There is no root aggregator POM or Maven wrapper. Use the module-specific commands. All four module verification jobs passed in [GitHub Actions run 36237680544](https://github.com/Manahil-Iftikhar/collaborative-editing-system/actions/runs/36237680544) on September 26, 2026.

Set `JWT_SECRET` in the user-service terminal as described below. Start all four modules in separate terminals using the commands in the [README](../README.md). Keep those terminals open.

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
| Maven failure | Confirm JDK selection, dependency access, and the module POM used |

If a browser restricts requests from local files, serve the repository through a local static server and open `collab-editor.html` there. Do not expose the development services publicly.

## Tests

```bash
mvn -f user-service/pom.xml test
mvn -f document-service/pom.xml test
mvn -f version-service/pom.xml test
```

Read the generated `target/surefire-reports/` within each service for actual execution results. There are 44 declared test methods: 39 existing service tests and five focused JWT tests. No gateway test class is present in the reviewed tree.

## Review scope

The documentation was checked against controllers, service implementations, POMs, configuration, browser API calls, and test declarations at source commit `9842a086550e18fec3934a5105b9491594462113`. The initial documentation refresh did not execute Java. A subsequent Java 17 CI run successfully built all four modules and ran the existing service suites without application-code changes.

## Automated checks

The [Java checks workflow](../.github/workflows/java-checks.yml) verifies each module independently on pushes and pull requests. Available Surefire reports are uploaded even when a job fails and retained for 14 days. The gateway has build coverage only; passing these jobs does not verify browser integration, authorization, or simultaneous editing.

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

The previous signing value remains in Git history. If it was used in a deployment, replace it in that environment. This change does not enforce authentication or document authorization: the existing request-permission configuration remains a separate gap.
