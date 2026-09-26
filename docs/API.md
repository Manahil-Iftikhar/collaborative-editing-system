# API reference

Gateway base URL: `http://localhost:8080`.

This reference mirrors controller mappings. It does not imply enforced authentication or end-to-end runtime verification.

## Users

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/api/users/register` | Register a user |
| POST | `/api/users/login` | Check credentials and return a token |
| GET | `/api/users/profile/{username}` | Read a profile |
| PUT | `/api/users/profile/{username}` | Update a profile |
| GET | `/api/users/{userId}` | Read a user by ID |

## Documents

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/api/documents` | Create a document |
| PUT | `/api/documents/{documentId}` | Replace content and record a change |
| GET | `/api/documents/{documentId}` | Read a document |
| GET | `/api/documents/{documentId}/changes` | Retrieve change records |
| GET | `/api/documents/owner/{ownerId}` | List documents by owner |
| GET | `/api/documents/public` | List documents marked public |

## Versions

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/api/versions` | Store a snapshot |
| POST | `/api/versions/revert` | Create a new snapshot from an earlier one |
| GET | `/api/versions/history/{documentId}` | List version history |
| GET | `/api/versions/{documentId}/{versionNumber}` | Read a specific snapshot |
| GET | `/api/versions/contributions/{documentId}` | Read contribution counters |

The revert endpoint expects **query parameters** `documentId`, `versionNumber`, and `userId`, not a JSON request body. For example:

```bash
curl -X POST "http://localhost:8080/api/versions/revert?documentId=1&versionNumber=1&userId=1"
```

Replace sample IDs with values returned from your local instance. A successful revert response describes a new snapshot; it does not mean the document service's current content has changed.

For request-body fields, consult the DTO classes in each service's `src/main/java/com/collab/dto/` directory. Controllers catch runtime errors and map them to HTTP responses; a shared validation/error contract is still a future improvement.
