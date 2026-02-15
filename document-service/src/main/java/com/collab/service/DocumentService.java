package com.collab.service;

import com.collab.dto.*;
import com.collab.model.Document;
import com.collab.model.DocumentChange;
import com.collab.repository.DocumentRepository;
import com.collab.repository.DocumentChangeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentChangeRepository documentChangeRepository;

    /**
     * Operation 1: Create a new document
     */
    public DocumentResponse createDocument(DocumentCreateRequest request) {
        Document document = new Document();
        document.setTitle(request.getTitle());
        document.setContent(request.getContent());
        document.setOwnerId(request.getOwnerId());
        document.setPublic(request.isPublic());
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        document.setLastEditedBy(request.getOwnerId());

        Document savedDocument = documentRepository.save(document);
        return mapToDocumentResponse(savedDocument);
    }

    /**
     * Operation 2: Edit an existing document collaboratively
     */
    public DocumentResponse editDocument(Long documentId, DocumentEditRequest request) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        // Update document content
        document.setContent(request.getContent());
        document.setUpdatedAt(LocalDateTime.now());
        document.setLastEditedBy(request.getUserId());

        // Track the change
        DocumentChange change = new DocumentChange(
            documentId,
            request.getUserId(),
            request.getChangeType() != null ? request.getChangeType() : "UPDATE",
            request.getContent(),
            request.getPosition()
        );
        documentChangeRepository.save(change);

        Document updatedDocument = documentRepository.save(document);
        return mapToDocumentResponse(updatedDocument);
    }

    /**
     * Operation 3: Track changes in real-time
     */
    public List<DocumentChange> getDocumentChanges(Long documentId) {
        return documentChangeRepository.findByDocumentIdOrderByTimestampDesc(documentId);
    }

    /**
     * Operation 4: Get document by ID
     */
    public DocumentResponse getDocument(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        return mapToDocumentResponse(document);
    }

    /**
     * Operation 5: Get all documents by owner
     */
    public List<DocumentResponse> getDocumentsByOwner(Long ownerId) {
        return documentRepository.findByOwnerId(ownerId).stream()
                .map(this::mapToDocumentResponse)
                .collect(Collectors.toList());
    }

    /**
     * Operation 6: Get all public documents
     */
    public List<DocumentResponse> getPublicDocuments() {
        return documentRepository.findByIsPublicTrue().stream()
                .map(this::mapToDocumentResponse)
                .collect(Collectors.toList());
    }

    private DocumentResponse mapToDocumentResponse(Document document) {
        return new DocumentResponse(
            document.getId(),
            document.getTitle(),
            document.getContent(),
            document.getOwnerId(),
            document.getCreatedAt(),
            document.getUpdatedAt(),
            document.getLastEditedBy(),
            document.isPublic()
        );
    }
}
