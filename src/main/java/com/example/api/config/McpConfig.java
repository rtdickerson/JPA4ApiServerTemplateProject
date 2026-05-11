package com.example.api.config;

import com.example.api.mcp.ItemMcpTools;
import com.example.api.mcp.McpResources;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.WebMvcSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration
@RequiredArgsConstructor
public class McpConfig {

    private final ItemMcpTools itemMcpTools;
    private final McpResources mcpResources;

    /**
     * SSE transport — exposes two HTTP endpoints:
     *   GET  /mcp/sse      ← clients subscribe here for server-sent events
     *   POST /mcp/message  ← clients send JSON-RPC messages here
     */
    @Bean
    public WebMvcSseServerTransportProvider mcpTransportProvider(ObjectMapper objectMapper) {
        return new WebMvcSseServerTransportProvider(objectMapper, "/mcp/message");
    }

    @Bean
    public McpSyncServer mcpSyncServer(WebMvcSseServerTransportProvider transportProvider) {
        return McpServer.sync(transportProvider)
                .serverInfo("item-api-server", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(Boolean.FALSE)
                        // true = server will send prompts/listChanged notifications
                        .prompts(Boolean.TRUE)
                        .resources(Boolean.FALSE, Boolean.FALSE)
                        .build())
                .tools(itemMcpTools.all())
                .resources(mcpResources.all())
                .build();
    }

    /** Wires the MCP SSE endpoint into Spring WebMVC's router. */
    @Bean
    public RouterFunction<ServerResponse> mcpRouterFunction(
            WebMvcSseServerTransportProvider transportProvider) {
        return transportProvider.getRouterFunction();
    }
}
