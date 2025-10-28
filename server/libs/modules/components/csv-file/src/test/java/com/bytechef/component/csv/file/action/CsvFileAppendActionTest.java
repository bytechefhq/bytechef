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

package com.bytechef.component.csv.file.action;

import static com.bytechef.component.csv.file.constant.CsvFileConstants.FILE_ENTRY;
import static com.bytechef.component.csv.file.constant.CsvFileConstants.ROWS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class CsvFileAppendActionTest {

    private ActionContext mockContext;
    private Parameters mockParameters;
    private FileEntry mockFileEntry;
    private ByteArrayOutputStream capturedOutputStream;

    @BeforeEach
    void setUp() {
        mockContext = mock(ActionContext.class);
        mockParameters = mock(Parameters.class);
        mockFileEntry = mock(FileEntry.class);
        capturedOutputStream = null;

        when(mockFileEntry.getName()).thenReturn("test.csv");
        when(mockParameters.getRequiredFileEntry(FILE_ENTRY)).thenReturn(mockFileEntry);
    }

    @Test
    void testAppendToExistingFileWithHeader() throws IOException {
        // File already has content (header written)
        byte[] existingBytes = "existing content".getBytes(StandardCharsets.UTF_8);

        // New rows to append
        List<Map<String, ?>> newRows = List.of(
            Map.of("column_1", 3, "column_2", "C", "column_3", "Z"),
            Map.of("column_1", 4, "column_2", "D", "column_3", "W"));

        setupMocks(existingBytes, newRows);

        // Execute
        FileEntry result = CsvFileAppendAction.perform(mockParameters, mockParameters, mockContext);

        // Verify
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(mockFileEntry);

        // Verify content - should NOT include header since file already has content
        String capturedContent = captureStoredContent();
        // Check that values are present (order may vary)
        assertThat(capturedContent).contains("3");
        assertThat(capturedContent).contains("C");
        assertThat(capturedContent).contains("Z");
        assertThat(capturedContent).contains("4");
        assertThat(capturedContent).contains("D");
        assertThat(capturedContent).contains("W");
    }

    @Test
    void testAppendToEmptyFile() throws IOException {
        // Empty file
        byte[] existingBytes = new byte[0];

        // New rows to append
        List<Map<String, ?>> newRows = List.of(
            Map.of("name", "John", "age", 30),
            Map.of("name", "Jane", "age", 25));

        setupMocks(existingBytes, newRows);

        // Execute
        FileEntry result = CsvFileAppendAction.perform(mockParameters, mockParameters, mockContext);

        // Verify
        assertThat(result).isNotNull();

        String capturedContent = captureStoredContent();
        // Should include header for empty file (order may vary)
        assertThat(capturedContent).contains("name");
        assertThat(capturedContent).contains("age");
        assertThat(capturedContent).contains("John");
        assertThat(capturedContent).contains("Jane");
        assertThat(capturedContent).contains("30");
        assertThat(capturedContent).contains("25");
    }

    @Test
    void testAppendToNullFile() throws IOException {
        // Null content (non-existent file)
        byte[] existingBytes = null;

        // New rows to append
        List<Map<String, ?>> newRows = List.of(
            Map.of("id", 1, "value", "test"));

        setupMocks(existingBytes, newRows);

        // Execute
        FileEntry result = CsvFileAppendAction.perform(mockParameters, mockParameters, mockContext);

        // Verify
        assertThat(result).isNotNull();

        String capturedContent = captureStoredContent();
        // Should include header for new file (order may vary)
        assertThat(capturedContent).contains("id");
        assertThat(capturedContent).contains("value");
        assertThat(capturedContent).contains("test");
        assertThat(capturedContent).contains("1");
    }

    @Test
    void testAppendEmptyRowsList() throws IOException {
        // Existing CSV content
        String existingContent = "header1,header2\nvalue1,value2\n";
        byte[] existingBytes = existingContent.getBytes(StandardCharsets.UTF_8);

        // Empty rows list
        List<Map<String, ?>> newRows = List.of();

        setupMocks(existingBytes, newRows);

        // Execute
        FileEntry result = CsvFileAppendAction.perform(mockParameters, mockParameters, mockContext);

        // Verify
        assertThat(result).isNotNull();

        String capturedContent = captureStoredContent();
        // No new content should be added
        assertThat(capturedContent).isEmpty();
    }

    @Test
    void testAppendWithSpecialCharacters() throws IOException {
        // Existing CSV content (file has content)
        String existingContent = "name,description\nProduct1,Simple\n";
        byte[] existingBytes = existingContent.getBytes(StandardCharsets.UTF_8);

        // New rows with special characters
        List<Map<String, ?>> newRows = List.of(
            Map.of("name", "Product2", "description", "Has,comma"),
            Map.of("name", "Product3", "description", "Has\"quote"));

        setupMocks(existingBytes, newRows);

        // Execute
        CsvFileAppendAction.perform(mockParameters, mockParameters, mockContext);

        // Verify - appended content only
        String capturedContent = captureStoredContent();
        assertThat(capturedContent).contains("Product2");
        assertThat(capturedContent).contains("Product3");
    }

    @Test
    void testAppendWithMixedDataTypes() throws IOException {
        // Existing CSV content
        String existingContent = "id,name,active,score\n1,Test,true,9.5\n";
        byte[] existingBytes = existingContent.getBytes(StandardCharsets.UTF_8);

        // New rows with mixed data types
        List<Map<String, ?>> newRows = List.of(
            Map.of("id", 2, "name", "Demo", "active", false, "score", 8.7),
            Map.of("id", 3, "name", "Sample", "active", true, "score", 10.0));

        setupMocks(existingBytes, newRows);

        // Execute
        CsvFileAppendAction.perform(mockParameters, mockParameters, mockContext);

        // Verify - appended content only (values may be in different order)
        String capturedContent = captureStoredContent();
        assertThat(capturedContent).contains("2");
        assertThat(capturedContent).contains("Demo");
        assertThat(capturedContent).contains("false");
        assertThat(capturedContent).contains("8.7");
        assertThat(capturedContent).contains("3");
        assertThat(capturedContent).contains("Sample");
        assertThat(capturedContent).contains("true");
        assertThat(capturedContent).contains("10.0");
    }

    @Test
    void testAppendMultipleRowsToExistingFile() throws IOException {
        // Existing CSV with multiple rows
        String existingContent = "col1,col2,col3\nrow1_1,row1_2,row1_3\nrow2_1,row2_2,row2_3\n";
        byte[] existingBytes = existingContent.getBytes(StandardCharsets.UTF_8);

        // Append multiple rows
        List<Map<String, ?>> newRows = List.of(
            Map.of("col1", "row3_1", "col2", "row3_2", "col3", "row3_3"),
            Map.of("col1", "row4_1", "col2", "row4_2", "col3", "row4_3"),
            Map.of("col1", "row5_1", "col2", "row5_2", "col3", "row5_3"));

        setupMocks(existingBytes, newRows);

        // Execute
        CsvFileAppendAction.perform(mockParameters, mockParameters, mockContext);

        // Verify - appended content only (values may be in different order)
        String capturedContent = captureStoredContent();
        long rowCount = capturedContent.lines()
            .count();

        // Should have 3 new rows
        assertThat(rowCount).isGreaterThanOrEqualTo(3);
        assertThat(capturedContent).contains("row3_1");
        assertThat(capturedContent).contains("row3_2");
        assertThat(capturedContent).contains("row3_3");
        assertThat(capturedContent).contains("row4_1");
        assertThat(capturedContent).contains("row4_2");
        assertThat(capturedContent).contains("row4_3");
        assertThat(capturedContent).contains("row5_1");
        assertThat(capturedContent).contains("row5_2");
        assertThat(capturedContent).contains("row5_3");
    }

    private void setupMocks(byte[] existingBytes, List<Map<String, ?>> newRows) throws IOException {
        capturedOutputStream = new ByteArrayOutputStream();

        // Mock file operations
        when(mockContext.file(any())).thenAnswer(invocation -> {
            Context.ContextFunction<Context.File, ?> function = invocation.getArgument(0);
            Context.File mockFile = mock(Context.File.class);

            // Mock getOutputStream - return our capturing stream
            when(mockFile.getOutputStream(any(FileEntry.class))).thenReturn(capturedOutputStream);

            // Mock getContentLength - return existing content length
            long contentLength = (existingBytes == null || existingBytes.length == 0) ? 0 : existingBytes.length;
            when(mockFile.getContentLength(any(FileEntry.class))).thenReturn(contentLength);

            return function.apply(mockFile);
        });

        // Mock parameters
        when(mockParameters.getList(eq(ROWS), any(TypeReference.class), eq(List.of())))
            .thenReturn(newRows);
    }

    private String captureStoredContent() {
        if (capturedOutputStream == null) {
            return "";
        }
        return capturedOutputStream.toString(StandardCharsets.UTF_8);
    }
}
