# Browser workflow verification

The gateway integration workflow runs a Chromium smoke check against the actual editor and all four backend services. The browser loads the editor from a temporary local HTTP server and sends requests to the gateway on another origin, exercising browser CORS behavior for the tested operations.

## Covered workflow

- Register and log in using the editor.
- Create a private document, save content, and reload it from the document list.
- Create a snapshot, open the Versions tab, and load that snapshot into the editor.
- Register and log in as another account; confirm browser-origin requests for the owner's private document and version history return 403.
- Verify titles and snapshot descriptions containing HTML/event-handler payloads display literally in both document lists, the document details, and snapshot history; no injected elements or execution markers may appear.
- Reject a stale save while preserving its draft, then reload the newer content.
- Fail on page errors and error dialogs encountered during this flow.

The runner also executes the existing HTTP checks and direct database-console probes. Its temporary users, signing secret and H2 data are isolated to the processes it starts.

## Reproduce locally

Use Java 17, Maven, Python 3.10+ and free ports 8080–8083. From the repository root:

```bash
python -m pip install -r requirements-browser.txt
python -m playwright install chromium
for module in user-service document-service version-service api-gateway; do
  mvn --batch-mode --no-transfer-progress -f "$module/pom.xml" -DskipTests package
done
python scripts/integration_smoke.py --browser
```

Linux environments may need Playwright system dependencies; CI installs them with `python -m playwright install --with-deps chromium`. Omit `--browser` to run only the standard-library HTTP checks.

## Evidence and limits

The [workflow](../.github/workflows/integration-smoke.yml) retains service logs, a JSON summary, a browser screenshot and a Playwright trace under `integration-results/` for 14 days. Traces may contain the disposable test credentials and tokens; this runner is intended only for its fresh local fixtures.

Consult the workflow result for whether a particular revision passed. The check covers one desktop Chromium flow, not all browsers, all UI features, accessibility, exhaustive hostile-input security, load capacity or deployment readiness. Loading a snapshot into the editor does not automatically persist it as current document content.

[Back to development guide](DEVELOPMENT.md)
