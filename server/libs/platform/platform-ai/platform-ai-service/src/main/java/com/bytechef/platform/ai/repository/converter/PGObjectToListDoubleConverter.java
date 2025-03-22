package com.bytechef.platform.ai.repository.converter;

import org.postgresql.util.PGobject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import java.util.Arrays;
import java.util.List;

@ReadingConverter
public class PGObjectToListDoubleConverter implements Converter<PGobject, List<Double>> {

    @Override
    public List<Double> convert(PGobject source) {
        try {
            String vectorString = source.getValue();

            if (vectorString.startsWith("[") && vectorString.endsWith("]")) {
                vectorString = vectorString.substring(1, vectorString.length() - 1);
            }

            return Arrays.stream(vectorString.split(","))
                .map(Double::parseDouble) // Convert to Double
                .toList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert PGobject to List<Double>", e);
        }
    }
}
