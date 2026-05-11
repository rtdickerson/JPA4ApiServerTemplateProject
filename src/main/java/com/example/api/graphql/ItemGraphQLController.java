package com.example.api.graphql;

import com.example.api.dto.CreateItemRequest;
import com.example.api.dto.ItemResponse;
import com.example.api.dto.UpdateItemRequest;
import com.example.api.service.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;

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
    public ItemResponse createItem(@Valid @Argument CreateItemRequest input) {
        return itemService.create(input);
    }

    @MutationMapping
    public ItemResponse updateItem(@Argument Long id, @Valid @Argument UpdateItemRequest input) {
        return itemService.update(id, input).orElse(null);
    }

    @MutationMapping
    public boolean deleteItem(@Argument Long id) {
        return itemService.delete(id);
    }
}
