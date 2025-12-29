/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.repository.converter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.postgresql.util.PGobject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Marko Kriskovic
 */
@ReadingConverter
@SuppressFBWarnings("EI_EXPOSE_REP2")
public class PGobjectToMapConverter implements Converter<PGobject, Map<String, Object>> {

    private final ObjectMapper objectMapper;

    public PGobjectToMapConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> convert(PGobject source) {
        try {
            return Map.copyOf(objectMapper.readValue(source.getValue(), Map.class));
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert PGobject to Map", e);
        }
    }
}
