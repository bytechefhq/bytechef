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

package com.bytechef.platform.configuration.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.file.storage.FileStorageServiceRegistry;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.platform.component.log.domain.LogEntry;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link EditorLogFileStorageReaderImpl}.
 *
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class EditorLogFileStorageReaderTest {

    private static final String EDITOR_LOG_DIR = "editor/logs";

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private ApplicationProperties.FileStorage fileStorage;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private FileStorageServiceRegistry fileStorageServiceRegistry;

    private EditorLogFileStorageReaderImpl editorLogFileStorageReader;

    @BeforeEach
    void beforeEach() {
        when(applicationProperties.getFileStorage()).thenReturn(fileStorage);
        when(fileStorage.getProvider()).thenReturn(ApplicationProperties.FileStorage.Provider.FILESYSTEM);
        when(fileStorageServiceRegistry.getFileStorageService("FILESYSTEM")).thenReturn(fileStorageService);

        editorLogFileStorageReader = new EditorLogFileStorageReaderImpl(
            applicationProperties, fileStorageServiceRegistry);
    }

    @Test
    void testReadLogEntriesByJobIdReturnsEmptyListWhenFileDoesNotExist() {
        long jobId = 1L;

        when(fileStorageService.fileExists(EDITOR_LOG_DIR, jobId + ".jsonl")).thenReturn(false);

        List<LogEntry> result = editorLogFileStorageReader.readLogEntriesByJobId(jobId);

        assertThat(result).isEmpty();
    }

    @Test
    void testReadLogEntriesByJobIdReturnsEmptyListOnException() {
        long jobId = 2L;

        when(fileStorageService.fileExists(EDITOR_LOG_DIR, jobId + ".jsonl"))
            .thenThrow(new RuntimeException("Storage unavailable"));

        List<LogEntry> result = editorLogFileStorageReader.readLogEntriesByJobId(jobId);

        assertThat(result).isEmpty();
    }

    @Test
    void testReadLogEntriesByJobIdReturnsEmptyListOnReadException() {
        long jobId = 3L;
        FileEntry fileEntry = new FileEntry("3.jsonl", "file://test/3.jsonl");

        when(fileStorageService.fileExists(EDITOR_LOG_DIR, jobId + ".jsonl")).thenReturn(true);
        when(fileStorageService.getFileEntry(EDITOR_LOG_DIR, jobId + ".jsonl")).thenReturn(fileEntry);
        when(fileStorageService.readFileToBytes(EDITOR_LOG_DIR, fileEntry))
            .thenThrow(new RuntimeException("Read failed"));

        List<LogEntry> result = editorLogFileStorageReader.readLogEntriesByJobId(jobId);

        assertThat(result).isEmpty();
    }

    @Test
    void testReadLogEntriesByJobIdReturnsEmptyListOnCorruptedContent() {
        long jobId = 4L;
        FileEntry fileEntry = new FileEntry("4.jsonl", "file://test/4.jsonl");

        when(fileStorageService.fileExists(EDITOR_LOG_DIR, jobId + ".jsonl")).thenReturn(true);
        when(fileStorageService.getFileEntry(EDITOR_LOG_DIR, jobId + ".jsonl")).thenReturn(fileEntry);
        when(fileStorageService.readFileToBytes(EDITOR_LOG_DIR, fileEntry))
            .thenReturn("not valid json".getBytes(StandardCharsets.UTF_8));

        List<LogEntry> result = editorLogFileStorageReader.readLogEntriesByJobId(jobId);

        assertThat(result).isEmpty();
    }

    @Test
    void testReadLogEntriesByJobIdParsesValidJsonl() {
        long jobId = 5L;
        FileEntry fileEntry = new FileEntry("5.jsonl", "file://test/5.jsonl");

        String jsonlContent =
            "{\"taskExecutionId\":100,\"level\":\"INFO\",\"componentName\":\"http\"," +
                "\"message\":\"first\",\"timestamp\":\"2026-01-01T00:00:00Z\"}\n" +
                "{\"taskExecutionId\":200,\"level\":\"WARN\",\"componentName\":\"http\"," +
                "\"message\":\"second\",\"timestamp\":\"2026-01-01T00:00:01Z\"}\n" +
                "{\"taskExecutionId\":100,\"level\":\"ERROR\",\"componentName\":\"http\"," +
                "\"message\":\"third\",\"timestamp\":\"2026-01-01T00:00:02Z\"}\n";

        when(fileStorageService.fileExists(EDITOR_LOG_DIR, jobId + ".jsonl")).thenReturn(true);
        when(fileStorageService.getFileEntry(EDITOR_LOG_DIR, jobId + ".jsonl")).thenReturn(fileEntry);
        when(fileStorageService.readFileToBytes(EDITOR_LOG_DIR, fileEntry))
            .thenReturn(jsonlContent.getBytes(StandardCharsets.UTF_8));

        List<LogEntry> allEntries = editorLogFileStorageReader.readLogEntriesByJobId(jobId);

        assertThat(allEntries).hasSize(3);
        assertThat(allEntries)
            .extracting(LogEntry::taskExecutionId)
            .containsExactly(100L, 200L, 100L);
    }

    @Test
    void testReadLogEntriesFiltersByTaskExecutionIdWithValidData() {
        long jobId = 6L;
        FileEntry fileEntry = new FileEntry("6.jsonl", "file://test/6.jsonl");

        String jsonlContent =
            "{\"taskExecutionId\":100,\"level\":\"INFO\",\"componentName\":\"http\"," +
                "\"message\":\"match\",\"timestamp\":\"2026-01-01T00:00:00Z\"}\n" +
                "{\"taskExecutionId\":200,\"level\":\"INFO\",\"componentName\":\"http\"," +
                "\"message\":\"no-match\",\"timestamp\":\"2026-01-01T00:00:01Z\"}\n" +
                "{\"taskExecutionId\":100,\"level\":\"WARN\",\"componentName\":\"http\"," +
                "\"message\":\"match-again\",\"timestamp\":\"2026-01-01T00:00:02Z\"}\n";

        when(fileStorageService.fileExists(EDITOR_LOG_DIR, jobId + ".jsonl")).thenReturn(true);
        when(fileStorageService.getFileEntry(EDITOR_LOG_DIR, jobId + ".jsonl")).thenReturn(fileEntry);
        when(fileStorageService.readFileToBytes(EDITOR_LOG_DIR, fileEntry))
            .thenReturn(jsonlContent.getBytes(StandardCharsets.UTF_8));

        List<LogEntry> filteredEntries = editorLogFileStorageReader.readLogEntries(jobId, 100L);

        assertThat(filteredEntries).hasSize(2);
        assertThat(filteredEntries)
            .extracting(LogEntry::taskExecutionId)
            .containsOnly(100L);
        assertThat(filteredEntries)
            .extracting(LogEntry::message)
            .containsExactly("match", "match-again");
    }

    @Test
    void testReadLogEntriesFiltersByTaskExecutionId() {
        long jobId = 5L;

        when(fileStorageService.fileExists(EDITOR_LOG_DIR, jobId + ".jsonl"))
            .thenThrow(new RuntimeException("Storage unavailable"));

        List<LogEntry> result = editorLogFileStorageReader.readLogEntries(jobId, 100L);

        assertThat(result).isEmpty();
    }

    @Test
    void testLogsExist() {
        long jobId = 6L;

        when(fileStorageService.fileExists(EDITOR_LOG_DIR, jobId + ".jsonl")).thenReturn(true);

        assertThat(editorLogFileStorageReader.logsExist(jobId)).isTrue();
    }

    @Test
    void testLogsExistReturnsFalse() {
        long jobId = 7L;

        when(fileStorageService.fileExists(EDITOR_LOG_DIR, jobId + ".jsonl")).thenReturn(false);

        assertThat(editorLogFileStorageReader.logsExist(jobId)).isFalse();
    }
}
