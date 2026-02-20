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
@ExtendWith(MockitoExtension.class)
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
