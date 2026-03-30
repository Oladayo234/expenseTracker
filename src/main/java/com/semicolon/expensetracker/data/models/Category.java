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
    private String name;
    private String icon;
    private boolean isDefault;
    private TransactionType transactionType;

    @ManyToOne(optional = true)
    @JoinColumn(name = "user_id")
    private User user;
}
