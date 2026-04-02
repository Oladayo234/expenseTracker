package com.semicolon.expensetracker.data.models;

import com.semicolon.expensetracker.utils.YearMonthConverter;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Budget {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;
    private BigDecimal monthlyTarget;

    @Column(name = "month_year")
    @Convert(converter = YearMonthConverter.class)
    private YearMonth monthYear;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
