# Architecture and implementation boundaries

## Request flow

The gateway forwards `/api/users/**`, `/api/documents/**`, and `/api/versions/**` to services on ports 8081, 8082, and 8083 respectively. It does not orchestrate a document-and-version transaction.

Each service follows controller → service → repository structure and owns an H2 database. Numeric user/document IDs are supplied across API boundaries; they are not cross-database foreign keys.

## Documents and versions

The document service updates the full content and writes a change record. That history can be retrieved with a GET request; it is not a push stream.

The version service stores snapshots independently and chooses the next version number from the current maximum. A revert copies an earlier snapshot into a new version record. There is no call to update the document service's current content.

Contribution counters are updated when snapshots or revert records are created. They should not be interpreted as verified authorship of every edit or a measure of text written.

## Authentication versus authorization

The user service hashes passwords with BCrypt and generates JWTs at login. However, its security chain uses `anyRequest().permitAll()`; no JWT request-authentication filter was found. Browser fetches do not attach an authorization header.

Login token generation alone does not protect profiles or documents. Caller-supplied owner/user IDs are not proof of identity. The public/private document flag is not an enforced access-control boundary.

## Development configuration

- Separate in-memory H2 stores are reset on service restart.
- H2 consoles are enabled.
- The gateway allows all origins and broad methods/headers.
- JWT signing requires an externally supplied `JWT_SECRET`; missing or short values prevent user-service startup.

Do not use real personal data or expose this configuration as a production service. If the former signing value in Git history was used in a deployment, replace it there before relying on token validation.

## Prioritized engineering roadmap

1. Enforce authenticated identity and per-resource authorization; add tests for denied access.
2. Establish production database/configuration profiles; signing material is now externally configured.
3. Define reliable document/snapshot coordination and rollback semantics.
4. Add optimistic concurrency checks and race-safe version numbering.
5. Implement and test a synchronization protocol if simultaneous editing is required.
6. Add input validation, consistent error responses, gateway integration tests, and browser tests.
7. Review dependency support and add reproducible CI before deployment.

These are future improvements, not features claimed by this revision.
