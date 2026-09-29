package com.collab;

import com.collab.dto.*;
import com.collab.model.Document;
import com.collab.model.DocumentChange;
import com.collab.repository.DocumentRepository;
import com.collab.repository.DocumentChangeRepository;
import com.collab.service.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class DocumentServiceTest {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentChangeRepository documentChangeRepository;

    private DocumentCreateRequest createRequest;

    @BeforeEach
    public void setUp() {
        documentRepository.deleteAll();
        documentChangeRepository.deleteAll();
        
        createRequest = new DocumentCreateRequest(
            "Test Document",
            "This is test content",
            1L,
            false
        );
    }

    private DocumentResponse editWithCurrentRevision(Long id, DocumentEditRequest request) {
        // Existing sequential fixtures always edit the revision they just read.
        request.setRevision(documentService.getDocument(id).getRevision());
        return documentService.editDocument(id, request);
    }

    @Test
    public void testCreateDocument_Success() {
        DocumentResponse response = documentService.createDocument(createRequest);

        assertNotNull(response);
        assertEquals("Test Document", response.getTitle());
        assertEquals("This is test content", response.getContent());
        assertEquals(1L, response.getOwnerId());
        assertFalse(response.isPublic());
        assertNotNull(response.getCreatedAt());
    }

    @Test
    public void testCreateDocument_PublicDocument() {
        createRequest.setPublic(true);
        DocumentResponse response = documentService.createDocument(createRequest);

        assertTrue(response.isPublic());
    }

    @Test
    public void testEditDocument_Success() {
        DocumentResponse created = documentService.createDocument(createRequest);

        DocumentEditRequest editRequest = new DocumentEditRequest(
            "Updated content",
            2L,
            "UPDATE",
            0
        );

        DocumentResponse edited = editWithCurrentRevision(created.getId(), editRequest);

        assertNotNull(edited);
        assertEquals("Updated content", edited.getContent());
        assertEquals(2L, edited.getLastEditedBy());
        assertNotNull(edited.getUpdatedAt());
    }

    @Test
    public void testEditDocument_NotFound() {
        DocumentEditRequest editRequest = new DocumentEditRequest(
            "Updated content",
            2L,
            "UPDATE",
            0
        );

        Exception exception = assertThrows(RuntimeException.class, () -> {
            editWithCurrentRevision(999L, editRequest);
        });

        assertEquals("Document not found", exception.getMessage());
    }

    @Test
    public void testEditDocument_TracksChanges() {
        DocumentResponse created = documentService.createDocument(createRequest);

        DocumentEditRequest editRequest = new DocumentEditRequest(
            "Updated content",
            2L,
            "INSERT",
            10
        );

        editWithCurrentRevision(created.getId(), editRequest);

        List<DocumentChange> changes = documentService.getDocumentChanges(created.getId());

        assertFalse(changes.isEmpty());
        assertEquals(1, changes.size());
        
        DocumentChange change = changes.get(0);
        assertEquals(created.getId(), change.getDocumentId());
        assertEquals(2L, change.getUserId());
        assertEquals("INSERT", change.getChangeType());
        assertEquals("Updated content", change.getChangeContent());
        assertEquals(10, change.getPosition());
    }

    @Test
    public void testGetDocumentChanges_MultipleEdits() {
        DocumentResponse created = documentService.createDocument(createRequest);

        // First edit
        DocumentEditRequest edit1 = new DocumentEditRequest("Edit 1", 1L, "UPDATE", 0);
        editWithCurrentRevision(created.getId(), edit1);

        // Second edit
        DocumentEditRequest edit2 = new DocumentEditRequest("Edit 2", 2L, "INSERT", 5);
        editWithCurrentRevision(created.getId(), edit2);

        // Third edit
        DocumentEditRequest edit3 = new DocumentEditRequest("Edit 3", 1L, "DELETE", 10);
        editWithCurrentRevision(created.getId(), edit3);

        List<DocumentChange> changes = documentService.getDocumentChanges(created.getId());

        assertEquals(3, changes.size());
        // Should be in descending order by timestamp
        assertEquals("DELETE", changes.get(0).getChangeType());
        assertEquals("INSERT", changes.get(1).getChangeType());
        assertEquals("UPDATE", changes.get(2).getChangeType());
    }

    @Test
    public void testGetDocument_Success() {
        DocumentResponse created = documentService.createDocument(createRequest);

        DocumentResponse retrieved = documentService.getDocument(created.getId());

        assertNotNull(retrieved);
        assertEquals(created.getId(), retrieved.getId());
        assertEquals("Test Document", retrieved.getTitle());
    }

    @Test
    public void testGetDocument_NotFound() {
        Exception exception = assertThrows(RuntimeException.class, () -> {
            documentService.getDocument(999L);
        });

        assertEquals("Document not found", exception.getMessage());
    }

    @Test
    public void testGetDocumentsByOwner() {
        // Create multiple documents for same owner
        documentService.createDocument(new DocumentCreateRequest("Doc 1", "Content 1", 1L, false));
        documentService.createDocument(new DocumentCreateRequest("Doc 2", "Content 2", 1L, false));
        documentService.createDocument(new DocumentCreateRequest("Doc 3", "Content 3", 2L, false));

        List<DocumentResponse> owner1Docs = documentService.getDocumentsByOwner(1L);
        List<DocumentResponse> owner2Docs = documentService.getDocumentsByOwner(2L);

        assertEquals(2, owner1Docs.size());
        assertEquals(1, owner2Docs.size());
    }

    @Test
    public void testGetPublicDocuments() {
        documentService.createDocument(new DocumentCreateRequest("Private Doc", "Content", 1L, false));
        documentService.createDocument(new DocumentCreateRequest("Public Doc 1", "Content", 1L, true));
        documentService.createDocument(new DocumentCreateRequest("Public Doc 2", "Content", 2L, true));

        List<DocumentResponse> publicDocs = documentService.getPublicDocuments();

        assertEquals(2, publicDocs.size());
        assertTrue(publicDocs.stream().allMatch(DocumentResponse::isPublic));
    }

    @Test
    public void testDocumentLastEditedBy() {
        DocumentResponse created = documentService.createDocument(createRequest);
        assertEquals(1L, created.getLastEditedBy());

        DocumentEditRequest edit1 = new DocumentEditRequest("Edit by user 2", 2L, "UPDATE", 0);
        DocumentResponse edited1 = editWithCurrentRevision(created.getId(), edit1);
        assertEquals(2L, edited1.getLastEditedBy());

        DocumentEditRequest edit2 = new DocumentEditRequest("Edit by user 3", 3L, "UPDATE", 0);
        DocumentResponse edited2 = editWithCurrentRevision(created.getId(), edit2);
        assertEquals(3L, edited2.getLastEditedBy());
    }

    @Test
    public void testCollaborativeEditing_MultipleUsers() {
        DocumentResponse created = documentService.createDocument(createRequest);

        // User 2 edits
        editWithCurrentRevision(created.getId(), 
            new DocumentEditRequest("User 2 content", 2L, "UPDATE", 0));

        // User 3 edits
        editWithCurrentRevision(created.getId(), 
            new DocumentEditRequest("User 3 content", 3L, "INSERT", 10));

        // User 2 edits again
        editWithCurrentRevision(created.getId(), 
            new DocumentEditRequest("User 2 more content", 2L, "UPDATE", 20));

        List<DocumentChange> changes = documentService.getDocumentChanges(created.getId());

        assertEquals(3, changes.size());
        
        // Verify different users contributed
        assertTrue(changes.stream().anyMatch(c -> c.getUserId() == 2L));
        assertTrue(changes.stream().anyMatch(c -> c.getUserId() == 3L));
    }
}
