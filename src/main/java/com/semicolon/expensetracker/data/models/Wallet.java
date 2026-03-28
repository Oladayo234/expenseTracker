package com.semicolon.expensetracker.data.models;

import com.semicolon.expensetracker.data.models.enums.Currency;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;
    private String name;
    private Currency currency;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
