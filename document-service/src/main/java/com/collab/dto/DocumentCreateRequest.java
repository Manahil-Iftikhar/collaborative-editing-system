package com.collab.dto;

public class DocumentCreateRequest {
    private String title;
    private String content;
    private Long ownerId;
    private boolean isPublic;

    public DocumentCreateRequest() {}

    public DocumentCreateRequest(String title, String content, Long ownerId, boolean isPublic) {
        this.title = title;
        this.content = content;
        this.ownerId = ownerId;
        this.isPublic = isPublic;
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

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }
}
