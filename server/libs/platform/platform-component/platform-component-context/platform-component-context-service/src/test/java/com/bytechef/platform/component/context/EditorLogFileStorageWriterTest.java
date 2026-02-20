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

package com.bytechef.platform.component.context;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.platform.component.log.domain.LogEntry;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link EditorLogFileStorageWriter}.
 *
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class EditorLogFileStorageWriterTest {

    private static final String EDITOR_LOG_DIR = "editor/logs";

    @Mock
    private FileStorageService fileStorageService;

    private EditorLogFileStorageWriter editorLogFileStorageWriter;

    @BeforeEach
    void beforeEach() {
        editorLogFileStorageWriter = new EditorLogFileStorageWriter(fileStorageService);
    }

    @Test
    void testStoreLogEntryCreatesNewFile() {
        long jobId = 1L;
        LogEntry logEntry = LogEntry.builder()
            .timestamp(Instant.now())
            .level(LogEntry.Level.INFO)
            .componentName("test")
            .taskExecutionId(100L)
            .message("Test message")
            .build();

        when(fileStorageService.fileExists(EDITOR_LOG_DIR, jobId + ".jsonl")).thenReturn(false);

        editorLogFileStorageWriter.storeLogEntry(jobId, 100L, logEntry);

        verify(fileStorageService, timeout(5000)).storeFileContent(
            eq(EDITOR_LOG_DIR), eq(jobId + ".jsonl"), any(byte[].class), eq(false));
    }

    @Test
    void testStoreLogEntryAppendsToExistingFile() {
        long jobId = 2L;
        LogEntry logEntry = LogEntry.builder()
            .timestamp(Instant.now())
            .level(LogEntry.Level.DEBUG)
            .componentName("test")
            .taskExecutionId(200L)
            .message("Appended message")
            .build();
        FileEntry existingFileEntry = new FileEntry("2.jsonl", "file://test/2.jsonl");

        when(fileStorageService.fileExists(EDITOR_LOG_DIR, jobId + ".jsonl")).thenReturn(true);
        when(fileStorageService.getFileEntry(EDITOR_LOG_DIR, jobId + ".jsonl")).thenReturn(existingFileEntry);
        when(fileStorageService.readFileToBytes(EDITOR_LOG_DIR, existingFileEntry))
            .thenReturn("existing content\n".getBytes());

        editorLogFileStorageWriter.storeLogEntry(jobId, 200L, logEntry);

        verify(fileStorageService, timeout(5000)).storeFileContent(
            eq(EDITOR_LOG_DIR), eq(jobId + ".jsonl"), any(byte[].class), eq(false));
    }

    @Test
    void testStoreLogEntryHandlesExceptionGracefully() {
        long jobId = 3L;
        LogEntry logEntry = LogEntry.builder()
            .timestamp(Instant.now())
            .level(LogEntry.Level.ERROR)
            .componentName("test")
            .taskExecutionId(300L)
            .message("Error message")
            .build();

        when(fileStorageService.fileExists(EDITOR_LOG_DIR, jobId + ".jsonl"))
            .thenThrow(new RuntimeException("Storage unavailable"));

        editorLogFileStorageWriter.storeLogEntry(jobId, 300L, logEntry);

        verify(fileStorageService, timeout(5000)).fileExists(EDITOR_LOG_DIR, jobId + ".jsonl");
        verify(fileStorageService, never()).storeFileContent(
            anyString(), anyString(), any(byte[].class), anyBoolean());
    }

    @Test
    void testDeleteLogEntries() {
        long jobId = 4L;
        FileEntry fileEntry = new FileEntry("4.jsonl", "file://test/4.jsonl");

        when(fileStorageService.fileExists(EDITOR_LOG_DIR, jobId + ".jsonl")).thenReturn(true);
        when(fileStorageService.getFileEntry(EDITOR_LOG_DIR, jobId + ".jsonl")).thenReturn(fileEntry);

        editorLogFileStorageWriter.deleteLogEntries(jobId);

        verify(fileStorageService).deleteFile(EDITOR_LOG_DIR, fileEntry);
    }

    @Test
    void testDeleteLogEntriesSkipsWhenFileDoesNotExist() {
        long jobId = 5L;

        when(fileStorageService.fileExists(EDITOR_LOG_DIR, jobId + ".jsonl")).thenReturn(false);

        editorLogFileStorageWriter.deleteLogEntries(jobId);

        verify(fileStorageService, never()).deleteFile(anyString(), any(FileEntry.class));
    }

    @Test
    void testDeleteLogEntriesHandlesExceptionGracefully() {
        long jobId = 6L;

        when(fileStorageService.fileExists(EDITOR_LOG_DIR, jobId + ".jsonl"))
            .thenThrow(new RuntimeException("Storage unavailable"));

        editorLogFileStorageWriter.deleteLogEntries(jobId);

        verify(fileStorageService, never()).deleteFile(anyString(), any(FileEntry.class));
    }

    @Test
    void testDeleteLogEntriesHandlesDeleteExceptionGracefully() {
        long jobId = 7L;
        FileEntry fileEntry = new FileEntry("7.jsonl", "file://test/7.jsonl");

        when(fileStorageService.fileExists(EDITOR_LOG_DIR, jobId + ".jsonl")).thenReturn(true);
        when(fileStorageService.getFileEntry(EDITOR_LOG_DIR, jobId + ".jsonl")).thenReturn(fileEntry);

        doThrow(new RuntimeException("Delete failed")).when(fileStorageService)
            .deleteFile(EDITOR_LOG_DIR, fileEntry);

        editorLogFileStorageWriter.deleteLogEntries(jobId);

        verify(fileStorageService).deleteFile(EDITOR_LOG_DIR, fileEntry);
    }
}
