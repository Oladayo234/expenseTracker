package com.semicolon.expensetracker.data.models;

import com.semicolon.expensetracker.data.models.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String icon;

    private boolean defaultCategory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;

    @ManyToOne(optional = true)
    @JoinColumn(name = "user_id")
    private User user;
}
