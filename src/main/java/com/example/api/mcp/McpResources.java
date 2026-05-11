package com.example.api.mcp;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@Slf4j
public class McpResources {

    private static final String MIME_MARKDOWN = "text/markdown";

    public List<McpServerFeatures.SyncResourceSpecification> all() {
        return List.of(
                apiDocs(),
                projectDocs()
        );
    }

    private McpServerFeatures.SyncResourceSpecification apiDocs() {
        var resource = new McpSchema.Resource(
                "resource:///api.md",
                "api.md",
                "API reference describing the available endpoints, tools, and usage patterns.",
                MIME_MARKDOWN,
                null
        );
        return new McpServerFeatures.SyncResourceSpecification(resource, (exchange, req) ->
                readClasspathResource("mcp/api.md", req.uri()));
    }

    private McpServerFeatures.SyncResourceSpecification projectDocs() {
        var resource = new McpSchema.Resource(
                "resource:///project.md",
                "project.md",
                "Project overview describing the system architecture, domain model, and design decisions.",
                MIME_MARKDOWN,
                null
        );
        return new McpServerFeatures.SyncResourceSpecification(resource, (exchange, req) ->
                readClasspathResource("mcp/project.md", req.uri()));
    }

    private static McpSchema.ReadResourceResult readClasspathResource(String path, String uri) {
        try {
            var bytes = new ClassPathResource(path).getInputStream().readAllBytes();
            var text = new String(bytes, StandardCharsets.UTF_8);
            return new McpSchema.ReadResourceResult(
                    List.of(new McpSchema.TextResourceContents(uri, MIME_MARKDOWN, text)));
        } catch (IOException e) {
            log.error("Failed to read MCP resource: {}", path, e);
            return new McpSchema.ReadResourceResult(
                    List.of(new McpSchema.TextResourceContents(uri, "text/plain",
                            "Error loading resource: " + e.getMessage())));
        }
    }
}
