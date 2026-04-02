package com.semicolon.expensetracker.utils;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.YearMonth;

@Converter(autoApply = true)
public class YearMonthConverter implements AttributeConverter<YearMonth, String> {
    @Override
    public String convertToDatabaseColumn(YearMonth yearMonth) {
        if (yearMonth == null) throw new IllegalArgumentException("YearMonth cannot be null");
        return yearMonth.toString();
    }

    @Override
    public YearMonth convertToEntityAttribute(String dbData) {
        if (dbData == null) throw new IllegalArgumentException("YearMonth data cannot be null");
        return YearMonth.parse(dbData);
    }

}
