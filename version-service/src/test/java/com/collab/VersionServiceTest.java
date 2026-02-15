package com.collab;

import com.collab.dto.VersionCreateRequest;
import com.collab.dto.VersionResponse;
import com.collab.model.DocumentVersion;
import com.collab.model.UserContribution;
import com.collab.repository.DocumentVersionRepository;
import com.collab.repository.UserContributionRepository;
import com.collab.service.VersionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class VersionServiceTest {

    @Autowired
    private VersionService versionService;

    @Autowired
    private DocumentVersionRepository versionRepository;

    @Autowired
    private UserContributionRepository contributionRepository;

    private VersionCreateRequest createRequest;

    @BeforeEach
    public void setUp() {
        versionRepository.deleteAll();
        contributionRepository.deleteAll();
        
        createRequest = new VersionCreateRequest(
            1L,
            "Initial content",
            1L,
            "Initial version"
        );
    }

    @Test
    public void testCreateVersion_FirstVersion() {
        VersionResponse response = versionService.createVersion(createRequest);

        assertNotNull(response);
        assertEquals(1L, response.getDocumentId());
        assertEquals(1, response.getVersionNumber());
        assertEquals("Initial content", response.getContent());
        assertEquals(1L, response.getCreatedBy());
        assertEquals("Initial version", response.getChangeDescription());
    }

    @Test
    public void testCreateVersion_IncrementVersionNumber() {
        versionService.createVersion(createRequest);

        VersionCreateRequest secondVersion = new VersionCreateRequest(
            1L,
            "Second version content",
            2L,
            "Added more content"
        );

        VersionResponse response = versionService.createVersion(secondVersion);

        assertEquals(2, response.getVersionNumber());
        assertEquals("Second version content", response.getContent());
    }

    @Test
    public void testCreateVersion_MultipleVersions() {
        versionService.createVersion(createRequest);
        versionService.createVersion(new VersionCreateRequest(1L, "Version 2", 2L, "Update"));
        VersionResponse third = versionService.createVersion(
            new VersionCreateRequest(1L, "Version 3", 3L, "More updates")
        );

        assertEquals(3, third.getVersionNumber());
    }

    @Test
    public void testGetVersionHistory() {
        versionService.createVersion(createRequest);
        versionService.createVersion(new VersionCreateRequest(1L, "Version 2", 2L, "Update 2"));
        versionService.createVersion(new VersionCreateRequest(1L, "Version 3", 1L, "Update 3"));

        List<VersionResponse> history = versionService.getVersionHistory(1L);

        assertEquals(3, history.size());
        // Should be in descending order
        assertEquals(3, history.get(0).getVersionNumber());
        assertEquals(2, history.get(1).getVersionNumber());
        assertEquals(1, history.get(2).getVersionNumber());
    }

    @Test
    public void testGetVersion_Success() {
        versionService.createVersion(createRequest);
        versionService.createVersion(new VersionCreateRequest(1L, "Version 2", 2L, "Update"));

        VersionResponse version = versionService.getVersion(1L, 2);

        assertNotNull(version);
        assertEquals(2, version.getVersionNumber());
        assertEquals("Version 2", version.getContent());
    }

    @Test
    public void testGetVersion_NotFound() {
        versionService.createVersion(createRequest);

        Exception exception = assertThrows(RuntimeException.class, () -> {
            versionService.getVersion(1L, 999);
        });

        assertEquals("Version not found", exception.getMessage());
    }

    @Test
    public void testRevertToVersion_Success() {
        versionService.createVersion(createRequest);
        versionService.createVersion(new VersionCreateRequest(1L, "Version 2", 2L, "Update"));
        versionService.createVersion(new VersionCreateRequest(1L, "Version 3", 3L, "Update"));

        VersionResponse reverted = versionService.revertToVersion(1L, 1, 2L);

        assertNotNull(reverted);
        assertEquals(4, reverted.getVersionNumber()); // New version created
        assertEquals("Initial content", reverted.getContent()); // Content from version 1
        assertEquals("Reverted to version 1", reverted.getChangeDescription());
        assertEquals(2L, reverted.getCreatedBy());
    }

    @Test
    public void testRevertToVersion_NotFound() {
        versionService.createVersion(createRequest);

        Exception exception = assertThrows(RuntimeException.class, () -> {
            versionService.revertToVersion(1L, 999, 2L);
        });

        assertEquals("Version not found", exception.getMessage());
    }

    @Test
    public void testRevertToVersion_PreservesHistory() {
        versionService.createVersion(createRequest);
        versionService.createVersion(new VersionCreateRequest(1L, "Version 2", 2L, "Update"));
        versionService.revertToVersion(1L, 1, 3L);

        List<VersionResponse> history = versionService.getVersionHistory(1L);

        assertEquals(3, history.size());
        assertEquals("Initial content", history.get(0).getContent()); // Reverted version
        assertEquals("Version 2", history.get(1).getContent()); // Original version 2
        assertEquals("Initial content", history.get(2).getContent()); // Original version 1
    }

    @Test
    public void testUserContributions_SingleUser() {
        versionService.createVersion(createRequest);
        versionService.createVersion(new VersionCreateRequest(1L, "Version 2", 1L, "Update"));

        List<UserContribution> contributions = versionService.getUserContributions(1L);

        assertEquals(1, contributions.size());
        
        UserContribution contribution = contributions.get(0);
        assertEquals(1L, contribution.getUserId());
        assertEquals(2, contribution.getVersionsCreated());
        assertEquals(2, contribution.getEditsCount());
    }

    @Test
    public void testUserContributions_MultipleUsers() {
        versionService.createVersion(createRequest); // User 1
        versionService.createVersion(new VersionCreateRequest(1L, "V2", 2L, "User 2 edit")); // User 2
        versionService.createVersion(new VersionCreateRequest(1L, "V3", 1L, "User 1 again")); // User 1
        versionService.createVersion(new VersionCreateRequest(1L, "V4", 3L, "User 3 edit")); // User 3
        versionService.createVersion(new VersionCreateRequest(1L, "V5", 2L, "User 2 again")); // User 2

        List<UserContribution> contributions = versionService.getUserContributions(1L);

        assertEquals(3, contributions.size());

        // Find each user's contribution
        UserContribution user1 = contributions.stream()
            .filter(c -> c.getUserId() == 1L)
            .findFirst().orElseThrow();
        UserContribution user2 = contributions.stream()
            .filter(c -> c.getUserId() == 2L)
            .findFirst().orElseThrow();
        UserContribution user3 = contributions.stream()
            .filter(c -> c.getUserId() == 3L)
            .findFirst().orElseThrow();

        assertEquals(2, user1.getVersionsCreated());
        assertEquals(2, user2.getVersionsCreated());
        assertEquals(1, user3.getVersionsCreated());
    }

    @Test
    public void testUserContributions_TracksRevert() {
        versionService.createVersion(createRequest);
        versionService.createVersion(new VersionCreateRequest(1L, "V2", 2L, "Update"));
        versionService.revertToVersion(1L, 1, 3L); // User 3 reverts

        List<UserContribution> contributions = versionService.getUserContributions(1L);

        UserContribution user3 = contributions.stream()
            .filter(c -> c.getUserId() == 3L)
            .findFirst().orElseThrow();

        assertEquals(1, user3.getVersionsCreated());
        assertEquals(1, user3.getEditsCount());
    }

    @Test
    public void testVersionHistory_DifferentDocuments() {
        versionService.createVersion(new VersionCreateRequest(1L, "Doc 1 V1", 1L, "Doc 1"));
        versionService.createVersion(new VersionCreateRequest(2L, "Doc 2 V1", 1L, "Doc 2"));
        versionService.createVersion(new VersionCreateRequest(1L, "Doc 1 V2", 2L, "Doc 1 update"));

        List<VersionResponse> doc1History = versionService.getVersionHistory(1L);
        List<VersionResponse> doc2History = versionService.getVersionHistory(2L);

        assertEquals(2, doc1History.size());
        assertEquals(1, doc2History.size());
    }

    @Test
    public void testCompleteVersionControlWorkflow() {
        // Create initial version
        VersionResponse v1 = versionService.createVersion(
            new VersionCreateRequest(1L, "Initial draft", 1L, "First commit")
        );
        assertEquals(1, v1.getVersionNumber());

        // User 2 makes changes
        VersionResponse v2 = versionService.createVersion(
            new VersionCreateRequest(1L, "Added introduction", 2L, "Added intro section")
        );
        assertEquals(2, v2.getVersionNumber());

        // User 3 makes changes
        VersionResponse v3 = versionService.createVersion(
            new VersionCreateRequest(1L, "Added conclusion", 3L, "Added conclusion")
        );
        assertEquals(3, v3.getVersionNumber());

        // Revert to version 2
        VersionResponse v4 = versionService.revertToVersion(1L, 2, 1L);
        assertEquals(4, v4.getVersionNumber());
        assertEquals("Added introduction", v4.getContent());

        // Verify history
        List<VersionResponse> history = versionService.getVersionHistory(1L);
        assertEquals(4, history.size());

        // Verify contributions
        List<UserContribution> contributions = versionService.getUserContributions(1L);
        assertEquals(3, contributions.size());
    }
}
