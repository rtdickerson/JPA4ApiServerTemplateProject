package com.example.api.mcp;

import com.example.api.dto.CreateItemRequest;
import com.example.api.dto.UpdateItemRequest;
import com.example.api.service.ItemService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * MCP tool registrations backed by ItemService.
 * Each tool maps 1-to-1 with a service operation so AI agents
 * get the same semantics as the REST and GraphQL surfaces.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ItemMcpTools {

    private final ItemService itemService;
    private final ObjectMapper objectMapper;

    public List<McpServerFeatures.SyncToolSpecification> all() {
        return List.of(
                listItems(),
                getItem(),
                searchItems(),
                createItem(),
                updateItem(),
                deleteItem()
        );
    }

    // ── Tool definitions ────────────────────────────────────────────────────

    private McpServerFeatures.SyncToolSpecification listItems() {
        var tool = new McpSchema.Tool(
                "list_items",
                "Return all items in the inventory.",
                emptySchema()
        );
        return new McpServerFeatures.SyncToolSpecification(tool, (exchange, args) -> {
            try {
                return ok(objectMapper.writeValueAsString(itemService.findAll()));
            } catch (Exception e) {
                return err(e);
            }
        });
    }

    private McpServerFeatures.SyncToolSpecification getItem() {
        var tool = new McpSchema.Tool(
                "get_item",
                "Get a single item by its numeric ID.",
                """
                {"type":"object","properties":{"id":{"type":"number","description":"Item ID"}},"required":["id"]}
                """
        );
        return new McpServerFeatures.SyncToolSpecification(tool, (exchange, args) -> {
            try {
                long id = toLong(args.get("id"));
                var result = itemService.findById(id);
                return ok(result.isPresent()
                        ? objectMapper.writeValueAsString(result.get())
                        : "null");
            } catch (Exception e) {
                return err(e);
            }
        });
    }

    private McpServerFeatures.SyncToolSpecification searchItems() {
        var tool = new McpSchema.Tool(
                "search_items",
                "Search items whose name contains the given string (case-insensitive).",
                """
                {"type":"object","properties":{"name":{"type":"string","description":"Name fragment to search for"}},"required":["name"]}
                """
        );
        return new McpServerFeatures.SyncToolSpecification(tool, (exchange, args) -> {
            try {
                var name = (String) args.get("name");
                return ok(objectMapper.writeValueAsString(itemService.searchByName(name)));
            } catch (Exception e) {
                return err(e);
            }
        });
    }

    private McpServerFeatures.SyncToolSpecification createItem() {
        var tool = new McpSchema.Tool(
                "create_item",
                "Create a new item in the inventory.",
                """
                {
                  "type":"object",
                  "properties":{
                    "name":{"type":"string"},
                    "description":{"type":"string"},
                    "price":{"type":"number","minimum":0},
                    "quantity":{"type":"integer","minimum":0}
                  },
                  "required":["name","price","quantity"]
                }
                """
        );
        return new McpServerFeatures.SyncToolSpecification(tool, (exchange, args) -> {
            try {
                var req = new CreateItemRequest(
                        (String) args.get("name"),
                        (String) args.get("description"),
                        new BigDecimal(args.get("price").toString()),
                        ((Number) args.get("quantity")).intValue()
                );
                return ok(objectMapper.writeValueAsString(itemService.create(req)));
            } catch (Exception e) {
                return err(e);
            }
        });
    }

    private McpServerFeatures.SyncToolSpecification updateItem() {
        var tool = new McpSchema.Tool(
                "update_item",
                "Partially update an existing item. Only provided fields are changed.",
                """
                {
                  "type":"object",
                  "properties":{
                    "id":{"type":"number","description":"Item ID"},
                    "name":{"type":"string"},
                    "description":{"type":"string"},
                    "price":{"type":"number","minimum":0},
                    "quantity":{"type":"integer","minimum":0}
                  },
                  "required":["id"]
                }
                """
        );
        return new McpServerFeatures.SyncToolSpecification(tool, (exchange, args) -> {
            try {
                long id = toLong(args.get("id"));
                var priceRaw = args.get("price");
                var req = new UpdateItemRequest(
                        (String) args.get("name"),
                        (String) args.get("description"),
                        priceRaw != null ? new BigDecimal(priceRaw.toString()) : null,
                        args.get("quantity") != null ? ((Number) args.get("quantity")).intValue() : null
                );
                var result = itemService.update(id, req);
                return ok(result.isPresent()
                        ? objectMapper.writeValueAsString(result.get())
                        : "null");
            } catch (Exception e) {
                return err(e);
            }
        });
    }

    private McpServerFeatures.SyncToolSpecification deleteItem() {
        var tool = new McpSchema.Tool(
                "delete_item",
                "Delete an item by its numeric ID. Returns true if deleted, false if not found.",
                """
                {"type":"object","properties":{"id":{"type":"number","description":"Item ID"}},"required":["id"]}
                """
        );
        return new McpServerFeatures.SyncToolSpecification(tool, (exchange, args) -> {
            try {
                long id = toLong(args.get("id"));
                return ok(String.valueOf(itemService.delete(id)));
            } catch (Exception e) {
                return err(e);
            }
        });
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private static String emptySchema() {
        return "{\"type\":\"object\",\"properties\":{}}";
    }

    private static long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        return Long.parseLong(value.toString());
    }

    private static McpSchema.CallToolResult ok(String text) {
        return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(text)), false);
    }

    private static McpSchema.CallToolResult err(Exception e) {
        log.error("MCP tool error", e);
        return new McpSchema.CallToolResult(
                List.of(new McpSchema.TextContent("Error: " + e.getMessage())), true);
    }
}
