package com.bytechef.platform.ai.repository.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.util.PGobject;
import org.springframework.data.convert.WritingConverter;
import org.springframework.core.convert.converter.Converter;

import java.sql.SQLException;
import java.util.Map;

@WritingConverter
public class MapToPGObjectConverter implements Converter<Map<String, Object>, PGobject> {
    private final ObjectMapper objectMapper;

    public MapToPGObjectConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public PGobject convert(Map<String, Object> source) {
        try {
            PGobject jsonObject = new PGobject();
            jsonObject.setType("jsonb");
            jsonObject.setValue(objectMapper.writeValueAsString(source));
            return jsonObject;
        } catch (SQLException | JsonProcessingException e) {
            throw new RuntimeException("Failed to convert Map to PGobject", e);
        }
    }
}
