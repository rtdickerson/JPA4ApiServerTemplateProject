package com.example.api.dto;

import com.example.api.entity.Prompt;

import java.time.LocalDateTime;

public record PromptResponse(
        Long id,
        String promptName,
        String promptDescription,
        String promptText,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PromptResponse from(Prompt p) {
        return new PromptResponse(
                p.getId(),
                p.getPromptName(),
                p.getPromptDescription(),
                p.getPromptText(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
