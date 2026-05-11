package com.example.api.repository;

import com.example.api.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByNameContainingIgnoreCase(String name);

    List<Item> findByPriceLessThanEqual(BigDecimal maxPrice);

    List<Item> findByQuantityGreaterThan(Integer minQuantity);
}
