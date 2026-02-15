package com.collab.repository;

import com.collab.model.DocumentChange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChangeRepository extends JpaRepository<DocumentChange, Long> {
    List<DocumentChange> findByDocumentIdOrderByTimestampDesc(Long documentId);
    List<DocumentChange> findByDocumentIdAndUserId(Long documentId, Long userId);
}
