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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private UUID publicId;

    private String username;
    private String name;
    private String password;
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency")
    private Currency currencyPreference;
    private String phoneNumber;

    @PrePersist
    private void generatePublicId() {
        if (publicId == null) publicId = UUID.randomUUID();
    }
}
