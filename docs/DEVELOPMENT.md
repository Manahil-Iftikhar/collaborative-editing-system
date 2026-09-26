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

There is no root aggregator POM or Maven wrapper. Use the module-specific commands. The commands above are instructions to run, not recorded successful builds from this documentation review.

Start all four modules in separate terminals using the commands in the [README](../README.md). Keep those terminals open.

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

Read the generated `target/surefire-reports/` within each service for actual execution results. There are 39 declared test methods across the three suites. No gateway test class is present in the reviewed tree.

## Review scope

The documentation was checked against controllers, service implementations, POMs, configuration, browser API calls, and test declarations at source commit `9842a086550e18fec3934a5105b9491594462113`. Java code was not changed or executed in this refresh.
