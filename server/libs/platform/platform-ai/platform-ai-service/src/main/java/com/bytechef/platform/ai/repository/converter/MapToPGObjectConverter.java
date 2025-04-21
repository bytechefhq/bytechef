/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.ai.repository.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.postgresql.util.PGobject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

/**
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
        } catch (SQLException | JsonProcessingException e) {
            throw new RuntimeException("Failed to convert Map to PGobject", e);
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Could not clone PGobject", e);
        }
    }
}
