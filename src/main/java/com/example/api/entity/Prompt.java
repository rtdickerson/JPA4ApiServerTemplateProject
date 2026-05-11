package com.example.api.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "prompts")
@Data
@NoArgsConstructor
public class Prompt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String promptName;

    @Column(length = 1000)
    private String promptDescription;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String promptText;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Prompt(Long id, String promptName, String promptDescription, String promptText) {
        this.id = id;
        this.promptName = promptName;
        this.promptDescription = promptDescription;
        this.promptText = promptText;
    }
}
