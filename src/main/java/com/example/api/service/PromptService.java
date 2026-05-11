package com.example.api.service;

import com.example.api.dto.CreatePromptRequest;
import com.example.api.dto.PromptResponse;
import com.example.api.dto.UpdatePromptRequest;
import com.example.api.entity.Prompt;
import com.example.api.repository.PromptRepository;
import io.modelcontextprotocol.server.McpServerFeatures.SyncPromptSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PromptService {

    private static final Pattern ARG_PATTERN = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    private final PromptRepository promptRepository;
    private final McpSyncServer mcpSyncServer;

    /** Register all persisted prompts with the MCP server on startup. */
    @PostConstruct
    void syncToMcp() {
        promptRepository.findAll().forEach(p -> mcpSyncServer.addPrompt(buildSpec(p)));
    }

    public List<PromptResponse> findAll() {
        return promptRepository.findAll().stream().map(PromptResponse::from).toList();
    }

    public Optional<PromptResponse> findById(Long id) {
        return promptRepository.findById(id).map(PromptResponse::from);
    }

    @Transactional
    public PromptResponse create(CreatePromptRequest req) {
        var prompt = new Prompt(null, req.promptName(), req.promptDescription(), req.promptText());
        var saved = promptRepository.save(prompt);
        mcpSyncServer.addPrompt(buildSpec(saved));
        mcpSyncServer.notifyPromptsListChanged();
        return PromptResponse.from(saved);
    }

    @Transactional
    public Optional<PromptResponse> update(Long id, UpdatePromptRequest req) {
        return promptRepository.findById(id).map(existing -> {
            var oldName = existing.getPromptName();
            if (req.promptName() != null)        existing.setPromptName(req.promptName());
            if (req.promptDescription() != null)  existing.setPromptDescription(req.promptDescription());
            if (req.promptText() != null)          existing.setPromptText(req.promptText());
            var saved = promptRepository.save(existing);
            mcpSyncServer.removePrompt(oldName);
            mcpSyncServer.addPrompt(buildSpec(saved));
            mcpSyncServer.notifyPromptsListChanged();
            return PromptResponse.from(saved);
        });
    }

    @Transactional
    public boolean delete(Long id) {
        return promptRepository.findById(id).map(p -> {
            promptRepository.delete(p);
            mcpSyncServer.removePrompt(p.getPromptName());
            mcpSyncServer.notifyPromptsListChanged();
            return true;
        }).orElse(false);
    }

    // ── MCP spec builder ────────────────────────────────────────────────────

    /**
     * Converts a persisted Prompt into an MCP SyncPromptSpecification.
     *
     * Any {{argName}} placeholder found in promptText is exposed as an optional
     * MCP prompt argument.  When prompts/get is called, argument values are
     * substituted before the text is returned as a USER message.
     */
    private SyncPromptSpecification buildSpec(Prompt prompt) {
        var argNames = extractArgNames(prompt.getPromptText());
        var arguments = argNames.stream()
                .map(name -> new McpSchema.PromptArgument(name, "Value for {{" + name + "}}", false))
                .toList();

        var mcpPrompt = new McpSchema.Prompt(
                prompt.getPromptName(),
                prompt.getPromptDescription(),
                arguments
        );

        return new SyncPromptSpecification(mcpPrompt,
                (McpSyncServerExchange exchange, McpSchema.GetPromptRequest request) -> {
                    var text = interpolate(prompt.getPromptText(), request.arguments());
                    var message = new McpSchema.PromptMessage(
                            McpSchema.Role.USER,
                            new McpSchema.TextContent(text)
                    );
                    return new McpSchema.GetPromptResult(prompt.getPromptDescription(), List.of(message));
                });
    }

    private static List<String> extractArgNames(String text) {
        var matcher = ARG_PATTERN.matcher(text);
        var names = new LinkedHashSet<String>();
        while (matcher.find()) names.add(matcher.group(1));
        return List.copyOf(names);
    }

    private static String interpolate(String text, Map<String, Object> args) {
        if (args == null || args.isEmpty()) return text;
        var result = text;
        for (var entry : args.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
        }
        return result;
    }
}
