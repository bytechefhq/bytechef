/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.repository.converter;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import org.postgresql.util.PGobject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

/**
 * @version ee
 *
 * @author Marko Kriskovic
 */
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
