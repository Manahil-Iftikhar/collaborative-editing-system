package com.collab.model;

import jakarta.persistence.*;

@Entity
@Table(name = "user_contributions")
public class UserContribution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long documentId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Integer versionsCreated = 0;

    @Column(nullable = false)
    private Integer editsCount = 0;

    @Column(nullable = false)
    private Integer linesAdded = 0;

    @Column(nullable = false)
    private Integer linesDeleted = 0;

    public UserContribution() {}

    public UserContribution(Long documentId, Long userId) {
        this.documentId = documentId;
        this.userId = userId;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getVersionsCreated() {
        return versionsCreated;
    }

    public void setVersionsCreated(Integer versionsCreated) {
        this.versionsCreated = versionsCreated;
    }

    public Integer getEditsCount() {
        return editsCount;
    }

    public void setEditsCount(Integer editsCount) {
        this.editsCount = editsCount;
    }

    public Integer getLinesAdded() {
        return linesAdded;
    }

    public void setLinesAdded(Integer linesAdded) {
        this.linesAdded = linesAdded;
    }

    public Integer getLinesDeleted() {
        return linesDeleted;
    }

    public void setLinesDeleted(Integer linesDeleted) {
        this.linesDeleted = linesDeleted;
    }

    public void incrementVersionsCreated() {
        this.versionsCreated++;
    }

    public void incrementEditsCount() {
        this.editsCount++;
    }
}
