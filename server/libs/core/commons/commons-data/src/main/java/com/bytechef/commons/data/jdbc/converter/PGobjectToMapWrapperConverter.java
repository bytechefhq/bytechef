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

package com.bytechef.commons.data.jdbc.converter;

import com.bytechef.commons.data.jdbc.wrapper.MapWrapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.postgresql.util.PGobject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Reads a Postgres {@code jsonb} column (returned by the JDBC driver as a {@link PGobject}) into a {@link MapWrapper}.
 * Pairs with {@link MapWrapperToPGObjectConverter} on the write side. The legacy {@link StringToMapWrapperConverter} is
 * still required for {@code TEXT}/{@code VARCHAR} JSON columns — register both readers so Spring Data JDBC can hydrate
 * domain objects regardless of the underlying column type.
 *
 * @author Ivica Cardic
 */
@ReadingConverter
public class PGobjectToMapWrapperConverter implements Converter<PGobject, MapWrapper> {

    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI2")
    public PGobjectToMapWrapperConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public MapWrapper convert(PGobject source) {
        String value = source.getValue();

        if (value == null || value.isBlank()) {
            return new MapWrapper();
        }

        Map<String, Object> map = objectMapper.readValue(value, new TypeReference<>() {});

        return new MapWrapper(map);
    }
}
