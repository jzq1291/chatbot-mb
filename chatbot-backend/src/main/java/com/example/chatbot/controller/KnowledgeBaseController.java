package com.example.chatbot.controller;

import com.example.chatbot.dto.PageResponse;
import com.example.chatbot.entity.KnowledgeBase;
import com.example.chatbot.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai/knowledge")
@RequiredArgsConstructor
public class KnowledgeBaseController {
    private final KnowledgeService knowledgeService;

    @PostMapping
    public ResponseEntity<KnowledgeBase> addKnowledge(@RequestBody KnowledgeBase knowledge) {
        return ResponseEntity.ok(knowledgeService.addKnowledge(knowledge));
    }

    @GetMapping("/search")
    public ResponseEntity<PageResponse<KnowledgeBase>> searchKnowledge(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(knowledgeService.searchByKeyword(keyword, page, size));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<PageResponse<KnowledgeBase>> getByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(knowledgeService.findByCategory(category, page, size));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteKnowledge(@PathVariable Long id) {
        knowledgeService.deleteKnowledge(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<KnowledgeBase> updateKnowledge(@PathVariable Long id, @RequestBody KnowledgeBase knowledge) {
        return ResponseEntity.ok(knowledgeService.updateKnowledge(id, knowledge));
    }
} 