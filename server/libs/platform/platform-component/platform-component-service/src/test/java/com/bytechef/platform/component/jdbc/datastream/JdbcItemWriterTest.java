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

package com.bytechef.platform.component.jdbc.datastream;

import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.COLUMNS;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.NAME;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.definition.datastream.FieldDefinition;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
class JdbcItemWriterTest {

    private JdbcItemWriter jdbcItemWriter;
    private Parameters inputParameters;
    private Parameters connectionParameters;
    private ClusterElementContext context;

    @BeforeAll
    static void beforeAll() {
        MapUtils.setObjectMapper(JsonMapper.builder()
            .build());
    }

    @BeforeEach
    void setUp() {
        jdbcItemWriter = new JdbcItemWriter(
            "jdbc:postgresql://%s:%s/%s", "org.postgresql.Driver");
        inputParameters = mock(Parameters.class);
        connectionParameters = mock(Parameters.class);
        context = mock(ClusterElementContext.class);
    }

    @Test
    void testGetFieldsReturnsFieldDefinitionsFromColumns() {
        List<Map<String, Object>> columns = List.of(
            Map.of(NAME, "id", TYPE, "INTEGER"),
            Map.of(NAME, "name", TYPE, "STRING"),
            Map.of(NAME, "active", TYPE, "BOOLEAN"));

        when(inputParameters.getList(eq(COLUMNS), any(TypeReference.class), eq(List.of()))).thenReturn(columns);

        List<FieldDefinition> fields = jdbcItemWriter.getFields(inputParameters, connectionParameters, context);

        assertThat(fields).hasSize(3);
        assertThat(fields.get(0)
            .name()).isEqualTo("id");
        assertThat(fields.get(0)
            .type()).isEqualTo(Long.class);
        assertThat(fields.get(1)
            .name()).isEqualTo("name");
        assertThat(fields.get(1)
            .type()).isEqualTo(String.class);
        assertThat(fields.get(2)
            .name()).isEqualTo("active");
        assertThat(fields.get(2)
            .type()).isEqualTo(Boolean.class);
    }

    @Test
    void testGetFieldsWithEmptyColumns() {
        when(inputParameters.getList(eq(COLUMNS), any(TypeReference.class), eq(List.of()))).thenReturn(List.of());

        List<FieldDefinition> fields = jdbcItemWriter.getFields(inputParameters, connectionParameters, context);

        assertThat(fields).isEmpty();
    }

    @Test
    void testGetFieldsWithNumericTypes() {
        List<Map<String, Object>> columns = List.of(
            Map.of(NAME, "bigint_col", TYPE, "BIGINT"),
            Map.of(NAME, "decimal_col", TYPE, "DECIMAL"),
            Map.of(NAME, "double_col", TYPE, "DOUBLE"),
            Map.of(NAME, "float_col", TYPE, "FLOAT"),
            Map.of(NAME, "real_col", TYPE, "REAL"));

        when(inputParameters.getList(eq(COLUMNS), any(TypeReference.class), eq(List.of()))).thenReturn(columns);

        List<FieldDefinition> fields = jdbcItemWriter.getFields(inputParameters, connectionParameters, context);

        assertThat(fields).hasSize(5);
        assertThat(fields.get(0)
            .type()).isEqualTo(Long.class);
        assertThat(fields.get(1)
            .type()).isEqualTo(Double.class);
        assertThat(fields.get(2)
            .type()).isEqualTo(Double.class);
        assertThat(fields.get(3)
            .type()).isEqualTo(Double.class);
        assertThat(fields.get(4)
            .type()).isEqualTo(Double.class);
    }

    @Test
    void testGetFieldsWithDateTimeTypes() {
        List<Map<String, Object>> columns = List.of(
            Map.of(NAME, "date_col", TYPE, "DATE"),
            Map.of(NAME, "time_col", TYPE, "TIME"),
            Map.of(NAME, "timestamp_col", TYPE, "TIMESTAMP"));

        when(inputParameters.getList(eq(COLUMNS), any(TypeReference.class), eq(List.of()))).thenReturn(columns);

        List<FieldDefinition> fields = jdbcItemWriter.getFields(inputParameters, connectionParameters, context);

        assertThat(fields).hasSize(3);
        assertThat(fields.get(0)
            .type()).isEqualTo(LocalDate.class);
        assertThat(fields.get(1)
            .type()).isEqualTo(LocalTime.class);
        assertThat(fields.get(2)
            .type()).isEqualTo(LocalDateTime.class);
    }

    @Test
    void testGetFieldsWithUnknownTypeDefaultsToString() {
        List<Map<String, Object>> columns = List.of(
            Map.of(NAME, "unknown_col", TYPE, "UNKNOWN_TYPE"),
            Map.of(NAME, "text_col", TYPE, "TEXT"));

        when(inputParameters.getList(eq(COLUMNS), any(TypeReference.class), eq(List.of()))).thenReturn(columns);

        List<FieldDefinition> fields = jdbcItemWriter.getFields(inputParameters, connectionParameters, context);

        assertThat(fields).hasSize(2);
        assertThat(fields.get(0)
            .type()).isEqualTo(String.class);
        assertThat(fields.get(1)
            .type()).isEqualTo(String.class);
    }

    @Test
    void testGetFieldsWithMissingTypeDefaultsToString() {
        List<Map<String, Object>> columns = List.of(
            Map.of(NAME, "col_without_type"));

        when(inputParameters.getList(eq(COLUMNS), any(TypeReference.class), eq(List.of()))).thenReturn(columns);

        List<FieldDefinition> fields = jdbcItemWriter.getFields(inputParameters, connectionParameters, context);

        assertThat(fields).hasSize(1);
        assertThat(fields.get(0)
            .name()).isEqualTo("col_without_type");
        assertThat(fields.get(0)
            .type()).isEqualTo(String.class);
    }
}
