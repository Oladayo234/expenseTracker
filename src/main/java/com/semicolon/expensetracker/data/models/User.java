package com.semicolon.expensetracker.data.models;

import com.semicolon.expensetracker.data.models.enums.Currency;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;
    private String username;
    private String name;
    private String password;
    private String email;
    private Currency currencyPreference;
}
