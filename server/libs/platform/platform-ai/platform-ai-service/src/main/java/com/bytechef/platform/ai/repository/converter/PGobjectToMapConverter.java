package com.bytechef.platform.ai.repository.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.util.PGobject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import java.util.Map;

@ReadingConverter
public class PGobjectToMapConverter implements Converter<PGobject, Map<String, Object>> {

    private final ObjectMapper objectMapper;

    public PGobjectToMapConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> convert(PGobject source) {
        try {
            return objectMapper.readValue(source.getValue(), Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert PGobject to Map", e);
        }
    }
}
