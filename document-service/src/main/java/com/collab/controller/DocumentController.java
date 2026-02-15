package com.collab.controller;

import com.collab.dto.*;
import com.collab.model.DocumentChange;
import com.collab.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    /**
     * REST API 1: Create a new document
     * POST /api/documents
     */
    @PostMapping
    public ResponseEntity<?> createDocument(@RequestBody DocumentCreateRequest request) {
        try {
            DocumentResponse response = documentService.createDocument(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * REST API 2: Edit an existing document
     * PUT /api/documents/{documentId}
     */
    @PutMapping("/{documentId}")
    public ResponseEntity<?> editDocument(
            @PathVariable Long documentId,
            @RequestBody DocumentEditRequest request) {
        try {
            DocumentResponse response = documentService.editDocument(documentId, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * REST API 3: Get document changes (track changes in real-time)
     * GET /api/documents/{documentId}/changes
     */
    @GetMapping("/{documentId}/changes")
    public ResponseEntity<?> getDocumentChanges(@PathVariable Long documentId) {
        try {
            List<DocumentChange> changes = documentService.getDocumentChanges(documentId);
            return ResponseEntity.ok(changes);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * REST API 4: Get document by ID
     * GET /api/documents/{documentId}
     */
    @GetMapping("/{documentId}")
    public ResponseEntity<?> getDocument(@PathVariable Long documentId) {
        try {
            DocumentResponse response = documentService.getDocument(documentId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * REST API 5: Get documents by owner
     * GET /api/documents/owner/{ownerId}
     */
    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<?> getDocumentsByOwner(@PathVariable Long ownerId) {
        try {
            List<DocumentResponse> documents = documentService.getDocumentsByOwner(ownerId);
            return ResponseEntity.ok(documents);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * REST API 6: Get all public documents
     * GET /api/documents/public
     */
    @GetMapping("/public")
    public ResponseEntity<?> getPublicDocuments() {
        try {
            List<DocumentResponse> documents = documentService.getPublicDocuments();
            return ResponseEntity.ok(documents);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    public static class ErrorResponse {
        private String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }
}
