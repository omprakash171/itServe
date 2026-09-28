package com.itserve.service;

import com.itserve.dto.ArticleRequest;
import com.itserve.dto.ArticleResponse;
import com.itserve.entity.KnowledgeArticle;
import com.itserve.entity.Role;
import com.itserve.entity.User;
import com.itserve.exception.ForbiddenActionException;
import com.itserve.exception.ResourceNotFoundException;
import com.itserve.repository.KnowledgeArticleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class KnowledgeBaseService {

    private final KnowledgeArticleRepository articleRepository;

    public KnowledgeBaseService(KnowledgeArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    @Transactional(readOnly = true)
    public List<ArticleResponse> list(String search) {
        List<KnowledgeArticle> articles = (search == null || search.isBlank())
                ? articleRepository.findByOrderByCreatedAtDesc()
                : articleRepository.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCaseOrderByCreatedAtDesc(
                        search, search);
        return articles.stream().map(ArticleResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ArticleResponse get(Long id) {
        return ArticleResponse.from(find(id));
    }

    @Transactional
    public ArticleResponse create(ArticleRequest request, User author) {
        requireAuthorRole(author);
        KnowledgeArticle article = new KnowledgeArticle();
        article.setTitle(request.title().trim());
        article.setCategory(request.category().trim());
        article.setContent(request.content().trim());
        article.setAuthor(author);
        return ArticleResponse.from(articleRepository.save(article));
    }

    @Transactional
    public ArticleResponse update(Long id, ArticleRequest request, User editor) {
        requireAuthorRole(editor);
        KnowledgeArticle article = find(id);
        article.setTitle(request.title().trim());
        article.setCategory(request.category().trim());
        article.setContent(request.content().trim());
        return ArticleResponse.from(articleRepository.save(article));
    }

    @Transactional
    public ArticleResponse vote(Long id, boolean helpful) {
        KnowledgeArticle article = find(id);
        if (helpful) {
            article.setHelpfulYes(article.getHelpfulYes() + 1);
        } else {
            article.setHelpfulNo(article.getHelpfulNo() + 1);
        }
        return ArticleResponse.from(articleRepository.save(article));
    }

    private void requireAuthorRole(User user) {
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.SUPPORT_AGENT) {
            throw new ForbiddenActionException("Only admins and support agents can manage knowledge articles");
        }
    }

    private KnowledgeArticle find(Long id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found"));
    }
}
