package com.collab.controller;

import com.collab.dto.*;
import com.collab.model.DocumentChange;
import com.collab.service.DocumentService;
import com.collab.security.RemoteIdentity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    @Autowired private DocumentService documents;
    @Autowired private RemoteIdentity identity;

    @PostMapping
    public ResponseEntity<DocumentResponse> createDocument(@RequestBody DocumentCreateRequest request,
            @RequestHeader(value="Authorization", required=false) String authorization) {
        request.setOwnerId(identity.requireUser(authorization));
        return ResponseEntity.status(HttpStatus.CREATED).body(documents.createDocument(request));
    }

    @PutMapping("/{documentId}")
    public DocumentResponse editDocument(@PathVariable Long documentId,
            @RequestBody DocumentEditRequest request,
            @RequestHeader(value="Authorization", required=false) String authorization) {
        Long user = identity.requireUser(authorization);
        requireOwner(find(documentId), user);
        request.setUserId(user);
        return documents.editDocument(documentId, request);
    }

    @GetMapping("/{documentId}/changes")
    public List<DocumentChange> getDocumentChanges(@PathVariable Long documentId,
            @RequestHeader(value="Authorization", required=false) String authorization) {
        Long user = identity.requireUser(authorization);
        requireOwner(find(documentId), user);
        return documents.getDocumentChanges(documentId);
    }

    @GetMapping("/{documentId}")
    public DocumentResponse getDocument(@PathVariable Long documentId,
            @RequestHeader(value="Authorization", required=false) String authorization) {
        DocumentResponse document = find(documentId);
        if (!document.isPublic()) requireOwner(document, identity.requireUser(authorization));
        return document;
    }

    @GetMapping("/owner/{ownerId}")
    public List<DocumentResponse> getDocumentsByOwner(@PathVariable Long ownerId,
            @RequestHeader(value="Authorization", required=false) String authorization) {
        if (!identity.requireUser(authorization).equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return documents.getDocumentsByOwner(ownerId);
    }

    @GetMapping("/public")
    public List<DocumentResponse> getPublicDocuments() {
        return documents.getPublicDocuments();
    }

    private DocumentResponse find(Long id) {
        try { return documents.getDocument(id); }
        catch (RuntimeException ex) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"); }
    }

    private void requireOwner(DocumentResponse document, Long user) {
        if (!user.equals(document.getOwnerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }
}
