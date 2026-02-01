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

package com.bytechef.component.airtable.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.airtable.datastream.AirtableItemWriter;
import com.bytechef.component.airtable.util.AirtableUtils.AirtableField;
import com.bytechef.component.airtable.util.AirtableUtils.AirtableTable;
import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.definition.datastream.FieldDefinition;
import com.bytechef.component.exception.ProviderException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class AirtableItemWriterTest {

    private AirtableItemWriter airtableItemWriter;
    private Parameters inputParameters;
    private Parameters connectionParameters;
    private ClusterElementContext context;
    private Http.Executor mockedExecutor;
    private Http.Response mockedResponse;

    @BeforeEach
    void setUp() {
        airtableItemWriter = new AirtableItemWriter();
        inputParameters = mock(Parameters.class);
        connectionParameters = mock(Parameters.class);
        context = mock(ClusterElementContext.class);
        mockedExecutor = mock(Http.Executor.class);
        mockedResponse = mock(Http.Response.class);

        when(inputParameters.getRequiredString("baseId")).thenReturn("base123");
        when(inputParameters.getRequiredString("tableId")).thenReturn("table456");
    }

    @Test
    void testGetFieldsReturnsFieldDefinitions() {
        Map<String, List<AirtableTable>> tablesMap = Map.of(
            "tables", List.of(new AirtableTable(
                "table456", "Test Table",
                List.of(
                    new AirtableField("fld1", "Name", "Name field", "singleLineText", null),
                    new AirtableField("fld2", "Email", "Email field", "email", null),
                    new AirtableField("fld3", "Count", "Count field", "count", null)),
                "description", "primaryField", List.of())));

        setupMocks(tablesMap);

        List<FieldDefinition> fields = airtableItemWriter.getFields(inputParameters, connectionParameters, context);

        assertThat(fields).hasSize(3);
        assertThat(fields.get(0)
            .name()).isEqualTo("Name");
        assertThat(fields.get(0)
            .type()).isEqualTo(String.class);
        assertThat(fields.get(1)
            .name()).isEqualTo("Email");
        assertThat(fields.get(1)
            .type()).isEqualTo(String.class);
        assertThat(fields.get(2)
            .name()).isEqualTo("Count");
        assertThat(fields.get(2)
            .type()).isEqualTo(Integer.class);
    }

    @Test
    void testGetFieldsSkipsCollaboratorFields() {
        Map<String, List<AirtableTable>> tablesMap = Map.of(
            "tables", List.of(new AirtableTable(
                "table456", "Test Table",
                List.of(
                    new AirtableField("fld1", "Title", "Title field", "singleLineText", null),
                    new AirtableField("fld2", "Assignee", "Assignee field", "singleCollaborator", null),
                    new AirtableField("fld3", "Reviewers", "Reviewers field", "multipleCollaborators", null)),
                "description", "primaryField", List.of())));

        setupMocks(tablesMap);

        List<FieldDefinition> fields = airtableItemWriter.getFields(inputParameters, connectionParameters, context);

        assertThat(fields).hasSize(1);
        assertThat(fields.get(0)
            .name()).isEqualTo("Title");
    }

    @Test
    void testGetFieldsThrowsExceptionWhenTableNotFound() {
        Map<String, List<AirtableTable>> tablesMap = Map.of(
            "tables", List.of(new AirtableTable(
                "otherTable", "Other Table",
                List.of(),
                "description", "primaryField", List.of())));

        setupMocks(tablesMap);

        assertThatThrownBy(() -> airtableItemWriter.getFields(inputParameters, connectionParameters, context))
            .isInstanceOf(ProviderException.BadRequestException.class)
            .hasMessage("Requested table does not exist");
    }

    @Test
    void testGetFieldsWithEmptyFields() {
        Map<String, List<AirtableTable>> tablesMap = Map.of(
            "tables", List.of(new AirtableTable(
                "table456", "Empty Table",
                List.of(),
                "description", "primaryField", List.of())));

        setupMocks(tablesMap);

        List<FieldDefinition> fields = airtableItemWriter.getFields(inputParameters, connectionParameters, context);

        assertThat(fields).isEmpty();
    }

    @Test
    void testGetFieldsHandlesAllFieldTypes() {
        Map<String, List<AirtableTable>> tablesMap = Map.of(
            "tables", List.of(new AirtableTable(
                "table456", "Test Table",
                List.of(
                    new AirtableField("fld1", "Number", "Number field", "number", null),
                    new AirtableField("fld2", "Checkbox", "Checkbox field", "checkbox", null),
                    new AirtableField("fld3", "Select", "Select field", "multipleSelects", null),
                    new AirtableField("fld4", "Percent", "Percent field", "percent", null)),
                "description", "primaryField", List.of())));

        setupMocks(tablesMap);

        List<FieldDefinition> fields = airtableItemWriter.getFields(inputParameters, connectionParameters, context);

        assertThat(fields).hasSize(4);
        assertThat(fields.get(0)
            .type()).isEqualTo(Number.class);
        assertThat(fields.get(1)
            .type()).isEqualTo(Boolean.class);
        assertThat(fields.get(2)
            .type()).isEqualTo(List.class);
        assertThat(fields.get(3)
            .type()).isEqualTo(Integer.class);
    }

    @SuppressWarnings("unchecked")
    private void setupMocks(Map<String, List<AirtableTable>> tablesMap) {
        when(context.http(any())).thenAnswer(invocation -> {
            Context.ContextFunction<Http, Http.Executor> function = invocation.getArgument(0);
            Http mockHttp = mock(Http.class);

            when(mockHttp.get(any(String.class))).thenReturn(mockedExecutor);
            when(mockedExecutor.configuration(any())).thenReturn(mockedExecutor);
            when(mockedExecutor.execute()).thenReturn(mockedResponse);
            when(mockedResponse.getBody(Map.class)).thenReturn((Map) tablesMap);
            when(mockedResponse.getBody(any(TypeReference.class))).thenReturn(tablesMap);

            return function.apply(mockHttp);
        });
    }
}
