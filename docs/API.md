# API reference

Gateway base URL: `http://localhost:8080`.

This reference mirrors controller mappings. User-profile endpoints now require a valid bearer token and owner identity. Document and version endpoints enforce the policies below; full browser/gateway integration has not been verified.

## Users

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/api/users/register` | Register a user |
| POST | `/api/users/login` | Check credentials and return a token |
| GET | `/api/users/profile/{username}` | Read a profile |
| PUT | `/api/users/profile/{username}` | Update a profile |
| GET | `/api/users/{userId}` | Read a user by ID |

User profile GET/PUT and user-by-ID GET require `Authorization: Bearer <token>`. Missing or invalid tokens return 401; another account's resource returns 403. Registration and login POSTs remain public. Tokens for deleted or inactive users are rejected.

`GET /api/users/me` returns the validated active account for the supplied bearer token.

## Documents

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/api/documents` | Create a document |
| PUT | `/api/documents/{documentId}` | Replace content and record a change |
| GET | `/api/documents/{documentId}` | Read a document |
| GET | `/api/documents/{documentId}/changes` | Retrieve change records |
| GET | `/api/documents/owner/{ownerId}` | List documents by owner |
| GET | `/api/documents/public` | List documents marked public |

Document creation and all edits require a bearer token. Owner/editor IDs are taken from the authenticated account; supplied IDs cannot grant access. Private document reads, owner listings, and change history require the owner. Public document reads and the public listing remain anonymous. Missing/invalid identity returns 401, another owner returns 403, and identity-service failure returns 503.

## Versions

**All version endpoints require a valid bearer token and document ownership**, even when the current document is public. Unknown documents are rejected; authorization-service failures deny access. Snapshot creation and revert use the verified account ID, not a supplied `userId`.

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/api/versions` | Store a snapshot |
| POST | `/api/versions/revert` | Create a new snapshot from an earlier one |
| GET | `/api/versions/history/{documentId}` | List version history |
| GET | `/api/versions/{documentId}/{versionNumber}` | Read a specific snapshot |
| GET | `/api/versions/contributions/{documentId}` | Read contribution counters |

The revert endpoint expects **query parameters** `documentId` and `versionNumber`, not a JSON request body. A legacy `userId` parameter is optional and ignored; identity comes from the bearer token. For example:

```bash
curl -X POST "http://localhost:8080/api/versions/revert?documentId=1&versionNumber=1" -H "Authorization: Bearer <your-token>"
```

Replace sample IDs with values returned from your local instance. A successful revert response describes a new snapshot; it does not mean the document service's current content has changed.

For request-body fields, consult the DTO classes in each service's `src/main/java/com/collab/dto/` directory. Controllers catch runtime errors and map them to HTTP responses; a shared validation/error contract is still a future improvement.

### Snapshot write conflicts

Create and revert return **409 Conflict** with a sanitized error if a database integrity constraint rejects the write. A unique `(document_id, version_number)` constraint prevents duplicate snapshot numbers. The failed transaction does not retain its snapshot or contribution increment. Reload history and retry deliberately; automatic retries are not implemented. This does not make document saves and snapshots one atomic operation.
