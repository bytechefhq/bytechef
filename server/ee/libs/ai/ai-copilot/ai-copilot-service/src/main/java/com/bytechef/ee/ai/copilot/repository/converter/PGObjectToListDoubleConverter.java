/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.repository.converter;

import java.util.Arrays;
import java.util.List;
import org.postgresql.util.PGobject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

/**
 * @version ee
 *
 * @author Marko Kriskovic
 */
@ReadingConverter
public class PGObjectToListDoubleConverter implements Converter<PGobject, List<Double>> {

    @Override
    public List<Double> convert(PGobject source) {
        try {
            String vectorString = source.getValue();

            if (vectorString == null) {
                throw new IllegalArgumentException("PGobject value cannot be null");
            }

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
