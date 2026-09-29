package com.collab.dto;

public class DocumentEditRequest {
    private String content;
    private Long revision;

    public Long getRevision() { return revision; }
    public void setRevision(Long revision) { this.revision = revision; }
    private Long userId;
    private String changeType;
    private Integer position;

    public DocumentEditRequest() {}

    public DocumentEditRequest(String content, Long userId, String changeType, Integer position) {
        this.content = content;
        this.userId = userId;
        this.changeType = changeType;
        this.position = position;
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

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }
}
