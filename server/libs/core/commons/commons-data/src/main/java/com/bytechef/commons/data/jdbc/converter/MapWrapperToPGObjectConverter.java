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
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.SQLException;
import org.postgresql.util.PGobject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import tools.jackson.databind.ObjectMapper;

/**
 * Writes a {@link MapWrapper} as a Postgres {@code jsonb}-typed {@link PGobject}. Required for any JSONB column that
 * uses {@link MapWrapper} as the field type — Postgres rejects an implicit {@code varchar→jsonb} cast, so the legacy
 * {@link MapWrapperToStringConverter} cannot be used for JSONB columns. JSONB-typed values are also implicitly
 * assignable to {@code text}/{@code varchar} columns, so this converter can replace {@link MapWrapperToStringConverter}
 * on the write path for both JSONB and TEXT columns.
 *
 * @author Ivica Cardic
 */
@WritingConverter
public class MapWrapperToPGObjectConverter implements Converter<MapWrapper, PGobject> {

    private final ObjectMapper objectMapper;

    public MapWrapperToPGObjectConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.rebuild()
            .changeDefaultVisibility(vc -> vc.with(Visibility.ANY))
            .build();
    }

    @Override
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public PGobject convert(MapWrapper source) {
        try {
            PGobject pgObject = new PGobject();

            pgObject.setType("jsonb");
            pgObject.setValue(source == null ? null : objectMapper.writeValueAsString(source.getMap()));

            return pgObject;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to write MapWrapper as JSONB", exception);
        }
    }
}
