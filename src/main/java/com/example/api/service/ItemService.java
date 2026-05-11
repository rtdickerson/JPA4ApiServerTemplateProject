package com.example.api.service;

import com.example.api.dto.CreateItemRequest;
import com.example.api.dto.ItemResponse;
import com.example.api.dto.UpdateItemRequest;
import com.example.api.entity.Item;
import com.example.api.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {

    private final ItemRepository itemRepository;

    public List<ItemResponse> findAll() {
        return itemRepository.findAll().stream()
                .map(ItemResponse::from)
                .toList();
    }

    public Optional<ItemResponse> findById(Long id) {
        return itemRepository.findById(id).map(ItemResponse::from);
    }

    public List<ItemResponse> searchByName(String name) {
        return itemRepository.findByNameContainingIgnoreCase(name).stream()
                .map(ItemResponse::from)
                .toList();
    }

    @Transactional
    public ItemResponse create(CreateItemRequest req) {
        var item = new Item(null, req.name(), req.description(), req.price(), req.quantity());
        return ItemResponse.from(itemRepository.save(item));
    }

    @Transactional
    public Optional<ItemResponse> update(Long id, UpdateItemRequest req) {
        return itemRepository.findById(id).map(item -> {
            if (req.name() != null) item.setName(req.name());
            if (req.description() != null) item.setDescription(req.description());
            if (req.price() != null) item.setPrice(req.price());
            if (req.quantity() != null) item.setQuantity(req.quantity());
            return ItemResponse.from(itemRepository.save(item));
        });
    }

    @Transactional
    public boolean delete(Long id) {
        if (!itemRepository.existsById(id)) return false;
        itemRepository.deleteById(id);
        return true;
    }
}
