package com.example.api.dto;

public record UpdatePromptRequest(
        String promptName,
        String promptDescription,
        String promptText
) {}
