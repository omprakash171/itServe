package com.itserve.repository;

import com.itserve.entity.KnowledgeArticle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticle, Long> {

    List<KnowledgeArticle> findByOrderByCreatedAtDesc();

    List<KnowledgeArticle> findByTitleContainingIgnoreCaseOrContentContainingIgnoreCaseOrderByCreatedAtDesc(
            String title, String content);
}
