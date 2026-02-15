package com.collab.dto;

import java.time.LocalDateTime;

public class VersionResponse {
    private Long id;
    private Long documentId;
    private Integer versionNumber;
    private String content;
    private Long createdBy;
    private LocalDateTime createdAt;
    private String changeDescription;

    public VersionResponse() {}

    public VersionResponse(Long id, Long documentId, Integer versionNumber, 
                          String content, Long createdBy, LocalDateTime createdAt, 
                          String changeDescription) {
        this.id = id;
        this.documentId = documentId;
        this.versionNumber = versionNumber;
        this.content = content;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.changeDescription = changeDescription;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getChangeDescription() {
        return changeDescription;
    }

    public void setChangeDescription(String changeDescription) {
        this.changeDescription = changeDescription;
    }
}
