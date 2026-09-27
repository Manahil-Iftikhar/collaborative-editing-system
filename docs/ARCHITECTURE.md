# Architecture and implementation boundaries

## Request flow

The gateway forwards `/api/users/**`, `/api/documents/**`, and `/api/versions/**` to services on ports 8081, 8082, and 8083 respectively. It does not orchestrate a document-and-version transaction.

Each service follows controller → service → repository structure and owns an H2 database. Numeric user/document IDs are supplied across API boundaries; they are not cross-database foreign keys.

## Documents and versions

The document service updates the full content and writes a change record. That history can be retrieved with a GET request; it is not a push stream.

The version service stores snapshots independently and chooses the next version number from the current maximum. A revert copies an earlier snapshot into a new version record. There is no call to update the document service's current content.

Contribution counters are updated when snapshots or revert records are created. They should not be interpreted as verified authorship of every edit or a measure of text written.

## Authentication versus authorization

The user service hashes passwords with BCrypt, generates JWTs at login, and validates bearer tokens on protected requests. Tokens must refer to an existing active account. Profile reads and updates are restricted to the authenticated username, and numeric user lookups are restricted to that account's ID. The editor sends its token when loading the profile. Other user-service routes are denied by default; registration and login POSTs remain public.

This protection is enforced in the user service, including direct calls to port 8081. Document endpoints now call the user service's `/api/users/me` with the bearer token. Owner IDs and editor IDs come from the validated response. Only owners can edit or view private content, owner listings, and change history; public document content remains readable. An unavailable identity service denies protected operations. The version service validates active user identity and document ownership through both services. All version routes are owner-only, including history for public documents. Snapshot creation and revert derive the actor from the authenticated account. Upstream failures deny access. These endpoint controls do not replace production transport, database, or infrastructure hardening.

## Development configuration

- Separate in-memory H2 stores are reset on service restart.
- H2 consoles are enabled.
- The gateway allows all origins and broad methods/headers.
- JWT signing requires an externally supplied `JWT_SECRET`; missing or short values prevent user-service startup.

Do not use real personal data or expose this configuration as a production service. If the former signing value in Git history was used in a deployment, replace it there before relying on token validation.

## Prioritized engineering roadmap

1. Add full cross-service and browser integration tests for owner-only policies, and define explicit sharing roles before enabling collaborative access.
2. Establish production database/configuration profiles; signing material is now externally configured.
3. Define reliable document/snapshot coordination and rollback semantics.
4. Add optimistic concurrency checks and race-safe version numbering.
5. Implement and test a synchronization protocol if simultaneous editing is required.
6. Add input validation, consistent error responses, gateway integration tests, and browser tests.
7. Review dependency support and add reproducible CI before deployment.

These are future improvements, not features claimed by this revision.
