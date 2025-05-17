package com.example.chatbot.controller;

import com.example.chatbot.entity.KnowledgeBase;
import com.example.chatbot.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<List<KnowledgeBase>> searchKnowledge(@RequestParam String keyword) {
        return ResponseEntity.ok(knowledgeService.searchByKeyword(keyword));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<KnowledgeBase>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(knowledgeService.findByCategory(category));
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