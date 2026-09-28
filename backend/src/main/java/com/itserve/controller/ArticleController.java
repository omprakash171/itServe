package com.itserve.controller;

import com.itserve.dto.ArticleRequest;
import com.itserve.dto.ArticleResponse;
import com.itserve.security.UserPrincipal;
import com.itserve.service.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/articles")
@Tag(name = "Knowledge Base", description = "Self-service articles")
public class ArticleController {

    public record VoteRequest(boolean helpful) {
    }

    private final KnowledgeBaseService knowledgeBaseService;

    public ArticleController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @GetMapping
    @Operation(summary = "List articles, optionally filtered by a text search")
    public List<ArticleResponse> list(@RequestParam(required = false) String search) {
        return knowledgeBaseService.list(search);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one article")
    public ArticleResponse get(@PathVariable Long id) {
        return knowledgeBaseService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT_AGENT')")
    @Operation(summary = "Create an article (ADMIN / SUPPORT_AGENT)")
    public ArticleResponse create(@Valid @RequestBody ArticleRequest request,
                                  @AuthenticationPrincipal UserPrincipal principal) {
        return knowledgeBaseService.create(request, principal.getUser());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT_AGENT')")
    @Operation(summary = "Update an article (ADMIN / SUPPORT_AGENT)")
    public ArticleResponse update(@PathVariable Long id, @Valid @RequestBody ArticleRequest request,
                                  @AuthenticationPrincipal UserPrincipal principal) {
        return knowledgeBaseService.update(id, request, principal.getUser());
    }

    @PostMapping("/{id}/vote")
    @Operation(summary = "Mark an article helpful or not")
    public Map<String, Integer> vote(@PathVariable Long id, @RequestBody VoteRequest request) {
        ArticleResponse article = knowledgeBaseService.vote(id, request.helpful());
        return Map.of("helpfulYes", article.helpfulYes(), "helpfulNo", article.helpfulNo());
    }
}
