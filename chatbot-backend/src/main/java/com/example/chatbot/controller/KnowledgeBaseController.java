package com.example.chatbot.controller;

import com.example.chatbot.dto.PageResponse;
import com.example.chatbot.entity.KnowledgeBase;
import com.example.chatbot.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/ai/knowledge")
@RequiredArgsConstructor
public class KnowledgeBaseController {
    private final KnowledgeService knowledgeService;

    @GetMapping
    public ResponseEntity<PageResponse<KnowledgeBase>> findAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(knowledgeService.findAll(page, size));
    }

    @GetMapping("/search")
    public ResponseEntity<PageResponse<KnowledgeBase>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(knowledgeService.search(keyword, page, size));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<PageResponse<KnowledgeBase>> findByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(knowledgeService.findByCategory(category, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<KnowledgeBase> findById(@PathVariable Long id) {
        return ResponseEntity.ok(knowledgeService.findById(id));
    }

    @PostMapping
    public ResponseEntity<KnowledgeBase> addKnowledge(@RequestBody KnowledgeBase knowledge) {
        return ResponseEntity.ok(knowledgeService.addKnowledge(knowledge));
    }

    @PutMapping("/{id}")
    public ResponseEntity<KnowledgeBase> updateKnowledge(
            @PathVariable Long id,
            @RequestBody KnowledgeBase knowledge) {
        return ResponseEntity.ok(knowledgeService.updateKnowledge(id, knowledge));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteKnowledge(@PathVariable Long id) {
        knowledgeService.deleteKnowledge(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/batch-import")
    @PreAuthorize("hasAnyRole('ROLE_KNOWLEDGEMANAGER','ROLE_ADMIN')")
    public ResponseEntity<Void> batchImport(@RequestBody List<KnowledgeBase> knowledgeList) {
        log.info("Received batch import request with {} items", knowledgeList.size());
        
        if (knowledgeList.isEmpty()) {
            log.warn("Batch import request received with empty list");
            return ResponseEntity.badRequest().build();
        }
        
        if (knowledgeList.size() > 200) {
            log.warn("Batch import request exceeded maximum limit of 200 items. Received: {}", knowledgeList.size());
            return ResponseEntity.badRequest().build();
        }
        
        try {
            knowledgeService.batchImport(knowledgeList);
            log.info("Successfully queued batch import request for processing");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error processing batch import request", e);
            return ResponseEntity.internalServerError().build();
        }
    }
} 