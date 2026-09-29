package com.collab;

import com.collab.dto.*;
import com.collab.model.Document;
import com.collab.repository.DocumentRepository;
import com.collab.repository.DocumentChangeRepository;
import com.collab.service.DocumentService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class DocumentConflictTest {
    @Autowired DocumentService service;
    @Autowired DocumentChangeRepository changes;
    @Autowired EntityManager entityManager;
    @SpyBean DocumentRepository documents;

    @BeforeEach void clean() {
        changes.deleteAll();
        documents.deleteAll();
    }

    DocumentResponse create() {
        return service.createDocument(new DocumentCreateRequest("Fixture", "initial", 1L, false));
    }
    DocumentEditRequest edit(String content, Long revision) {
        var request = new DocumentEditRequest(content, 1L, "UPDATE", 0);
        request.setRevision(revision);
        return request;
    }

    @Test void staleAndMissingRevisionsLeaveContentAndHistoryUntouched() {
        var original = create();
        var missing = assertThrows(ResponseStatusException.class,
            () -> service.editDocument(original.getId(), edit("missing", null)));
        assertEquals(428, missing.getStatusCode().value());
        var saved = service.editDocument(original.getId(), edit("winner", original.getRevision()));
        assertEquals(original.getRevision() + 1, saved.getRevision());
        assertThrows(OptimisticLockingFailureException.class,
            () -> service.editDocument(original.getId(), edit("stale", original.getRevision())));
        assertEquals("winner", service.getDocument(original.getId()).getContent());
        assertEquals(1, service.getDocumentChanges(original.getId()).size());
        // A deliberate reload enables another save.
        var next = service.editDocument(original.getId(), edit("reconciled", saved.getRevision()));
        assertEquals(saved.getRevision() + 1, next.getRevision());
    }

    @Test void simultaneousEditsCommitOneDocumentAndOneChangeRecord() throws Exception {
        var original = create();
        var barrier = new CyclicBarrier(2);
        // Load real managed entities in each service transaction, then align the
        // reads so both transactions compete with the same entity revision.
        doAnswer(invocation -> {
            Document document = entityManager.find(Document.class, original.getId());
            barrier.await(10, TimeUnit.SECONDS);
            return Optional.of(document);
        }).when(documents).findById(original.getId());
        var pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> attempt(original, "first"));
            var second = pool.submit(() -> attempt(original, "second"));
            int successes = (first.get(20, TimeUnit.SECONDS) ? 1 : 0)
                + (second.get(20, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, successes);
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
            reset(documents);
        }
        var history = service.getDocumentChanges(original.getId());
        var saved = service.getDocument(original.getId());
        assertEquals(original.getRevision() + 1, saved.getRevision());
        assertEquals(1, history.size());
        assertEquals(saved.getContent(), history.get(0).getChangeContent());
    }

    boolean attempt(DocumentResponse original, String content) {
        try {
            service.editDocument(original.getId(), edit(content, original.getRevision()));
            return true;
        } catch (OptimisticLockingFailureException expected) {
            return false;
        }
    }
}
