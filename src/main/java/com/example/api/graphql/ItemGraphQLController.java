package com.example.api.graphql;

import com.example.api.dto.CreateItemRequest;
import com.example.api.dto.ItemResponse;
import com.example.api.dto.UpdateItemRequest;
import com.example.api.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ItemGraphQLController {

    private final ItemService itemService;

    @QueryMapping
    public List<ItemResponse> items() {
        return itemService.findAll();
    }

    @QueryMapping
    public ItemResponse item(@Argument Long id) {
        return itemService.findById(id).orElse(null);
    }

    @QueryMapping
    public List<ItemResponse> searchItems(@Argument String name) {
        return itemService.searchByName(name);
    }

    @MutationMapping
    public ItemResponse createItem(@Argument Map<String, Object> input) {
        var req = new CreateItemRequest(
                (String) input.get("name"),
                (String) input.get("description"),
                new BigDecimal(input.get("price").toString()),
                (Integer) input.get("quantity")
        );
        return itemService.create(req);
    }

    @MutationMapping
    public ItemResponse updateItem(@Argument Long id, @Argument Map<String, Object> input) {
        var priceRaw = input.get("price");
        var req = new UpdateItemRequest(
                (String) input.get("name"),
                (String) input.get("description"),
                priceRaw != null ? new BigDecimal(priceRaw.toString()) : null,
                (Integer) input.get("quantity")
        );
        return itemService.update(id, req).orElse(null);
    }

    @MutationMapping
    public boolean deleteItem(@Argument Long id) {
        return itemService.delete(id);
    }
}
