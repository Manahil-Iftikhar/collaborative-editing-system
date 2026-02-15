package com.collab.dto;

import java.time.LocalDateTime;

public class DocumentResponse {
    private Long id;
    private String title;
    private String content;
    private Long ownerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long lastEditedBy;
    private boolean isPublic;

    public DocumentResponse() {}

    public DocumentResponse(Long id, String title, String content, Long ownerId,
                           LocalDateTime createdAt, LocalDateTime updatedAt,
                           Long lastEditedBy, boolean isPublic) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastEditedBy = lastEditedBy;
        this.isPublic = isPublic;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getLastEditedBy() {
        return lastEditedBy;
    }

    public void setLastEditedBy(Long lastEditedBy) {
        this.lastEditedBy = lastEditedBy;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }
}
