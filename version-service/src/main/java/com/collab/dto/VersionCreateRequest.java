package com.collab.dto;

public class VersionCreateRequest {
    private Long documentId;
    private String content;
    private Long userId;
    private String changeDescription;

    public VersionCreateRequest() {}

    public VersionCreateRequest(Long documentId, String content, Long userId, String changeDescription) {
        this.documentId = documentId;
        this.content = content;
        this.userId = userId;
        this.changeDescription = changeDescription;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getChangeDescription() {
        return changeDescription;
    }

    public void setChangeDescription(String changeDescription) {
        this.changeDescription = changeDescription;
    }
}
