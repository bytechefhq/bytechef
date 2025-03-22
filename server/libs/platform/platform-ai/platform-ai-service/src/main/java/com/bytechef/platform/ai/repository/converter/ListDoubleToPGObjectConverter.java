package com.bytechef.platform.ai.repository.converter;

import org.postgresql.util.PGobject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

@WritingConverter
public class ListDoubleToPGObjectConverter implements Converter<List<Double>, PGobject> {

    @Override
    public PGobject convert(List<Double> source) {
        try {
            PGobject pgObject = new PGobject();
            pgObject.setType("vector(1536)");
            // Convert List<Double> to a comma-separated string representation
            String vectorString = source.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
            pgObject.setValue(vectorString);
            return pgObject;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to convert List<Double> to PGobject", e);
        }
    }
}
