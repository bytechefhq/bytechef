/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.repository.converter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.postgresql.util.PGobject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Marko Kriskovic
 */
@WritingConverter
@SuppressFBWarnings("EI_EXPOSE_REP2")
public class MapToPGObjectConverter implements Converter<Map<String, Object>, PGobject> {

    private final ObjectMapper objectMapper;

    public MapToPGObjectConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public PGobject convert(Map<String, Object> source) {
        Map<String, Object> defensiveCopy = new HashMap<>(source);
        try {
            PGobject jsonObject = new PGobject();

            jsonObject.setType("jsonb");
            jsonObject.setValue(objectMapper.writeValueAsString(defensiveCopy));

            return (PGobject) jsonObject.clone();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to convert Map to PGobject", e);
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Could not clone PGobject", e);
        }
    }
}
