package com.example.api;

import com.example.api.entity.Item;
import com.example.api.entity.Prompt;
import com.example.api.repository.ItemRepository;
import com.example.api.repository.PromptRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    CommandLineRunner seedData(ItemRepository items, PromptRepository prompts) {
        return args -> {
            if (items.count() == 0) {
                items.save(new Item(null, "Widget A", "Basic aluminum widget", new BigDecimal("9.99"), 100));
                items.save(new Item(null, "Widget B", "Advanced carbon-fiber widget", new BigDecimal("24.99"), 50));
                items.save(new Item(null, "Gadget X", "Multipurpose smart gadget", new BigDecimal("49.99"), 25));
                items.save(new Item(null, "Gizmo Pro", "Professional-grade gizmo", new BigDecimal("99.99"), 10));
            }
            if (prompts.count() == 0) {
                prompts.save(new Prompt(null,
                        "summarize_item",
                        "Summarize an inventory item in one sentence",
                        "Summarize the following inventory item in one concise sentence.\n\nItem name: {{name}}\nDescription: {{description}}\nPrice: ${{price}}\nQuantity in stock: {{quantity}}"));
                prompts.save(new Prompt(null,
                        "reorder_recommendation",
                        "Recommend a reorder quantity for an item",
                        "You are an inventory manager. Based on the current stock level of {{quantity}} units for '{{name}}' (priced at ${{price}} each), recommend an optimal reorder quantity and explain your reasoning briefly."));
                prompts.save(new Prompt(null,
                        "price_analysis",
                        "Analyse whether an item is competitively priced",
                        "Analyse whether ${{price}} is a competitive price for '{{name}}'. Assume a typical retail context and provide a one-paragraph assessment."));
            }
        };
    }
}
