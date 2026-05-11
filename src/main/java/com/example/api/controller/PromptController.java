package com.example.api.controller;

import com.example.api.dto.CreatePromptRequest;
import com.example.api.dto.PromptResponse;
import com.example.api.dto.UpdatePromptRequest;
import com.example.api.service.PromptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/prompts")
@RequiredArgsConstructor
public class PromptController {

    private final PromptService promptService;

    @GetMapping
    public List<PromptResponse> listAll() {
        return promptService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromptResponse> getById(@PathVariable Long id) {
        return promptService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PromptResponse> create(@Valid @RequestBody CreatePromptRequest req) {
        var created = promptService.create(req);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PromptResponse> update(
            @PathVariable Long id,
            @RequestBody UpdatePromptRequest req) {
        return promptService.update(id, req)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return promptService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
