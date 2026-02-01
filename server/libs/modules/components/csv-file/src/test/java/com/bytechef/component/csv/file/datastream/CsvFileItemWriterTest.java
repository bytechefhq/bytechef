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

package com.bytechef.component.csv.file.datastream;

import static com.bytechef.component.csv.file.constant.CsvFileConstants.FILE_ENTRY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.datastream.FieldDefinition;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class CsvFileItemWriterTest {

    private CsvFileItemWriter csvFileItemWriter;
    private Parameters inputParameters;
    private Parameters connectionParameters;
    private ClusterElementContext context;
    private FileEntry mockFileEntry;

    @BeforeEach
    void setUp() {
        csvFileItemWriter = new CsvFileItemWriter();
        inputParameters = mock(Parameters.class);
        connectionParameters = mock(Parameters.class);
        context = mock(ClusterElementContext.class);
        mockFileEntry = mock(FileEntry.class);

        when(inputParameters.getRequiredFileEntry(FILE_ENTRY)).thenReturn(mockFileEntry);
    }

    @Test
    void testGetFieldsFromExistingCsvFile() {
        String csvContent = "name,email,age\nJohn,john@example.com,30\nJane,jane@example.com,25";

        setupMocks(csvContent, csvContent.length());

        List<FieldDefinition> fields = csvFileItemWriter.getFields(inputParameters, connectionParameters, context);

        assertThat(fields).hasSize(3);
        assertThat(fields.get(0)
            .name()).isEqualTo("name");
        assertThat(fields.get(0)
            .label()).isEqualTo("name");
        assertThat(fields.get(0)
            .type()).isEqualTo(String.class);
        assertThat(fields.get(1)
            .name()).isEqualTo("email");
        assertThat(fields.get(2)
            .name()).isEqualTo("age");
    }

    @Test
    void testGetFieldsFromEmptyFile() {
        String csvContent = "";

        setupMocks(csvContent, 0);

        List<FieldDefinition> fields = csvFileItemWriter.getFields(inputParameters, connectionParameters, context);

        assertThat(fields).isEmpty();
    }

    @Test
    void testGetFieldsFromFileWithOnlyHeaderRow() {
        String csvContent = "id,status,value";

        setupMocks(csvContent, csvContent.length());

        List<FieldDefinition> fields = csvFileItemWriter.getFields(inputParameters, connectionParameters, context);

        assertThat(fields).hasSize(3);
        assertThat(fields.get(0)
            .name()).isEqualTo("id");
        assertThat(fields.get(1)
            .name()).isEqualTo("status");
        assertThat(fields.get(2)
            .name()).isEqualTo("value");
    }

    @Test
    void testGetFieldsStripsWhitespace() {
        String csvContent = "  first_name  ,  last_name  ,  email  \nJohn,Doe,john@example.com";

        setupMocks(csvContent, csvContent.length());

        List<FieldDefinition> fields = csvFileItemWriter.getFields(inputParameters, connectionParameters, context);

        assertThat(fields).hasSize(3);
        assertThat(fields.get(0)
            .name()).isEqualTo("first_name");
        assertThat(fields.get(1)
            .name()).isEqualTo("last_name");
        assertThat(fields.get(2)
            .name()).isEqualTo("email");
    }

    @Test
    void testGetFieldsWithEmptyHeaderLine() {
        String csvContent = "\nJohn,Doe,john@example.com";

        setupMocks(csvContent, csvContent.length());

        List<FieldDefinition> fields = csvFileItemWriter.getFields(inputParameters, connectionParameters, context);

        assertThat(fields).isEmpty();
    }

    @Test
    void testGetFieldsWithSingleColumn() {
        String csvContent = "id\n1\n2\n3";

        setupMocks(csvContent, csvContent.length());

        List<FieldDefinition> fields = csvFileItemWriter.getFields(inputParameters, connectionParameters, context);

        assertThat(fields).hasSize(1);
        assertThat(fields.get(0)
            .name()).isEqualTo("id");
    }

    private void setupMocks(String csvContent, long contentLength) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));

        when(context.file(any())).thenAnswer(invocation -> {
            Context.ContextFunction<Context.File, ?> function = invocation.getArgument(0);
            Context.File mockFile = mock(Context.File.class);

            when(mockFile.getInputStream(any(FileEntry.class))).thenReturn(inputStream);
            when(mockFile.getContentLength(any(FileEntry.class))).thenReturn(contentLength);

            return function.apply(mockFile);
        });
    }
}
