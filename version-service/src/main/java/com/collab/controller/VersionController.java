package com.collab.controller;

import com.collab.dto.VersionCreateRequest;
import com.collab.dto.VersionResponse;
import com.collab.model.UserContribution;
import com.collab.service.VersionService;
import com.collab.security.DocumentOwnerAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/versions")
public class VersionController {

    @Autowired
    private VersionService versionService;

    @Autowired
    private DocumentOwnerAccess access;

    /**
     * REST API 1: Create a new version
     * POST /api/versions
     */
    @PostMapping
    public ResponseEntity<?> createVersion(@RequestBody VersionCreateRequest request,
            @RequestHeader(value="Authorization", required=false) String authorization) {
        request.setUserId(access.requireOwner(request.getDocumentId(), authorization));
        try {
            VersionResponse response = versionService.createVersion(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * REST API 2: Revert to a previous version
     * POST /api/versions/revert
     */
    @PostMapping("/revert")
    public ResponseEntity<?> revertToVersion(
            @RequestParam Long documentId,
            @RequestParam Integer versionNumber,
            @RequestParam(required=false) Long userId,
            @RequestHeader(value="Authorization", required=false) String authorization) {
        userId = access.requireOwner(documentId, authorization);
        try {
            VersionResponse response = versionService.revertToVersion(documentId, versionNumber, userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * REST API 3: Get user contributions for a document
     * GET /api/versions/contributions/{documentId}
     */
    @GetMapping("/contributions/{documentId}")
    public ResponseEntity<?> getUserContributions(@PathVariable Long documentId,
            @RequestHeader(value="Authorization", required=false) String authorization) {
        access.requireOwner(documentId, authorization);
        try {
            List<UserContribution> contributions = versionService.getUserContributions(documentId);
            return ResponseEntity.ok(contributions);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * REST API 4: Get version history
     * GET /api/versions/history/{documentId}
     */
    @GetMapping("/history/{documentId}")
    public ResponseEntity<?> getVersionHistory(@PathVariable Long documentId,
            @RequestHeader(value="Authorization", required=false) String authorization) {
        access.requireOwner(documentId, authorization);
        try {
            List<VersionResponse> history = versionService.getVersionHistory(documentId);
            return ResponseEntity.ok(history);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * REST API 5: Get specific version
     * GET /api/versions/{documentId}/{versionNumber}
     */
    @GetMapping("/{documentId}/{versionNumber}")
    public ResponseEntity<?> getVersion(
            @PathVariable Long documentId,
            @PathVariable Integer versionNumber,
            @RequestHeader(value="Authorization", required=false) String authorization) {
        access.requireOwner(documentId, authorization);
        try {
            VersionResponse response = versionService.getVersion(documentId, versionNumber);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
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
