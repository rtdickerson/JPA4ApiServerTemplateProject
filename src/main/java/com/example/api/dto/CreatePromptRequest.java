package com.example.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreatePromptRequest(
        @NotBlank String promptName,
        String promptDescription,
        @NotBlank String promptText
) {}
