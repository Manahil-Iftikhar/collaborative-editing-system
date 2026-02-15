package com.collab.service;

import com.collab.dto.VersionCreateRequest;
import com.collab.dto.VersionResponse;
import com.collab.model.DocumentVersion;
import com.collab.model.UserContribution;
import com.collab.repository.DocumentVersionRepository;
import com.collab.repository.UserContributionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VersionService {

    @Autowired
    private DocumentVersionRepository versionRepository;

    @Autowired
    private UserContributionRepository contributionRepository;

    /**
     * Operation 1: Maintain version history of documents
     */
    @Transactional
    public VersionResponse createVersion(VersionCreateRequest request) {
        // Get the next version number
        Integer maxVersion = versionRepository.findMaxVersionNumber(request.getDocumentId());
        Integer nextVersion = (maxVersion != null) ? maxVersion + 1 : 1;

        // Create new version
        DocumentVersion version = new DocumentVersion(
            request.getDocumentId(),
            nextVersion,
            request.getContent(),
            request.getUserId(),
            request.getChangeDescription()
        );

        DocumentVersion savedVersion = versionRepository.save(version);

        // Update user contribution
        updateUserContribution(request.getDocumentId(), request.getUserId());

        return mapToVersionResponse(savedVersion);
    }

    /**
     * Operation 2: Revert to previous document versions
     */
    @Transactional
    public VersionResponse revertToVersion(Long documentId, Integer versionNumber, Long userId) {
        // Get the version to revert to
        DocumentVersion targetVersion = versionRepository
                .findByDocumentIdAndVersionNumber(documentId, versionNumber)
                .orElseThrow(() -> new RuntimeException("Version not found"));

        // Create a new version with the content from the target version
        Integer maxVersion = versionRepository.findMaxVersionNumber(documentId);
        Integer nextVersion = maxVersion + 1;

        DocumentVersion revertedVersion = new DocumentVersion(
            documentId,
            nextVersion,
            targetVersion.getContent(),
            userId,
            "Reverted to version " + versionNumber
        );

        DocumentVersion savedVersion = versionRepository.save(revertedVersion);
        updateUserContribution(documentId, userId);

        return mapToVersionResponse(savedVersion);
    }

    /**
     * Operation 3: Track user contributions
     */
    public List<UserContribution> getUserContributions(Long documentId) {
        return contributionRepository.findByDocumentId(documentId);
    }

    /**
     * Operation 4: Get version history
     */
    public List<VersionResponse> getVersionHistory(Long documentId) {
        return versionRepository.findByDocumentIdOrderByVersionNumberDesc(documentId)
                .stream()
                .map(this::mapToVersionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Operation 5: Get specific version
     */
    public VersionResponse getVersion(Long documentId, Integer versionNumber) {
        DocumentVersion version = versionRepository
                .findByDocumentIdAndVersionNumber(documentId, versionNumber)
                .orElseThrow(() -> new RuntimeException("Version not found"));
        return mapToVersionResponse(version);
    }

    private void updateUserContribution(Long documentId, Long userId) {
        UserContribution contribution = contributionRepository
                .findByDocumentIdAndUserId(documentId, userId)
                .orElse(new UserContribution(documentId, userId));

        contribution.incrementVersionsCreated();
        contribution.incrementEditsCount();
        contributionRepository.save(contribution);
    }

    private VersionResponse mapToVersionResponse(DocumentVersion version) {
        return new VersionResponse(
            version.getId(),
            version.getDocumentId(),
            version.getVersionNumber(),
            version.getContent(),
            version.getCreatedBy(),
            version.getCreatedAt(),
            version.getChangeDescription()
        );
    }
}
