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

package com.bytechef.platform.component.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.exception.FileStorageException;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.platform.component.log.domain.LogEntry;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Covers the per-task file layout and the ordered, awaitable write chain of the production {@link LogFileStorage}.
 *
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class LogFileStorageTest {

    private static final long JOB_ID = 1L;
    private static final String JOB_DIR = "logs/component_execution/1";
    private static final String LEGACY_DIR = "logs/component_execution";
    private static final String LEGACY_FILENAME = "1.jsonl";

    @Mock
    private FileStorageService fileStorageService;

    private LogFileStorage logFileStorage;

    @BeforeEach
    void beforeEach() {
        logFileStorage = new LogFileStorageImpl(fileStorageService);
    }

    @Test
    void testStoreLogEntriesWritesEachTaskExecutionToItsOwnFile() {
        when(fileStorageService.fileExists(JOB_DIR, "10.jsonl")).thenReturn(false);
        when(fileStorageService.fileExists(JOB_DIR, "20.jsonl")).thenReturn(false);
        when(fileStorageService.getFileEntries(JOB_DIR + "/")).thenReturn(Set.of());
        when(fileStorageService.fileExists(LEGACY_DIR, LEGACY_FILENAME)).thenReturn(false);

        logFileStorage.storeLogEntries(JOB_ID, 10L, List.of(logEntry(10L, "task ten")));
        logFileStorage.storeLogEntries(JOB_ID, 20L, List.of(logEntry(20L, "task twenty")));

        assertFalse(logFileStorage.logsExist(JOB_ID));

        verify(fileStorageService).storeFileContent(eq(JOB_DIR), eq("10.jsonl"), any(byte[].class), eq(false));
        verify(fileStorageService).storeFileContent(eq(JOB_DIR), eq("20.jsonl"), any(byte[].class), eq(false));
        verify(fileStorageService, never()).readFileToBytes(anyString(), any(FileEntry.class));
    }

    @Test
    void testWritesForAJobAreAppliedInSubmissionOrder() {
        FileEntry existingFile = new FileEntry("10.jsonl", "file://test/1/10.jsonl");

        when(fileStorageService.fileExists(JOB_DIR, "10.jsonl")).thenReturn(false, true);
        when(fileStorageService.getFileEntry(JOB_DIR, "10.jsonl")).thenReturn(existingFile);
        when(fileStorageService.readFileToBytes(JOB_DIR, existingFile))
            .thenReturn(jsonLine(logEntry(10L, "first")).getBytes(StandardCharsets.UTF_8));
        when(fileStorageService.getFileEntries(JOB_DIR + "/")).thenReturn(Set.of());
        when(fileStorageService.fileExists(LEGACY_DIR, LEGACY_FILENAME)).thenReturn(false);

        logFileStorage.storeLogEntries(JOB_ID, 10L, List.of(logEntry(10L, "first")));
        logFileStorage.storeLogEntries(JOB_ID, 10L, List.of(logEntry(10L, "second")));

        logFileStorage.readLogEntriesByJobId(JOB_ID);

        ArgumentCaptor<byte[]> contentArgumentCaptor = ArgumentCaptor.forClass(byte[].class);

        verify(fileStorageService, times(2)).storeFileContent(
            eq(JOB_DIR), eq("10.jsonl"), contentArgumentCaptor.capture(), eq(false));

        String finalContent = new String(contentArgumentCaptor.getAllValues()
            .get(1), StandardCharsets.UTF_8);

        assertTrue(finalContent.indexOf("first") < finalContent.indexOf("second"));
    }

    @Test
    void testAFailedWriteDoesNotStallLaterWritesForTheJob() {
        when(fileStorageService.fileExists(JOB_DIR, "10.jsonl"))
            .thenThrow(new FileStorageException("storage down"))
            .thenReturn(false);
        when(fileStorageService.getFileEntries(JOB_DIR + "/")).thenReturn(Set.of());
        when(fileStorageService.fileExists(LEGACY_DIR, LEGACY_FILENAME)).thenReturn(false);

        logFileStorage.storeLogEntries(JOB_ID, 10L, List.of(logEntry(10L, "first")));
        logFileStorage.storeLogEntries(JOB_ID, 10L, List.of(logEntry(10L, "second")));

        logFileStorage.readLogEntriesByJobId(JOB_ID);

        ArgumentCaptor<byte[]> contentArgumentCaptor = ArgumentCaptor.forClass(byte[].class);

        verify(fileStorageService, times(1)).storeFileContent(
            eq(JOB_DIR), eq("10.jsonl"), contentArgumentCaptor.capture(), eq(false));

        String content = new String(contentArgumentCaptor.getValue(), StandardCharsets.UTF_8);

        assertTrue(content.contains("second"));
        assertFalse(content.contains("first"));
    }

    @Test
    void testReadLogEntriesByJobIdCombinesTaskFilesWithTheLegacyJobFile() {
        FileEntry listedTaskFile = new FileEntry("/10.jsonl", "file://test/1//10.jsonl");
        FileEntry resolvedTaskFile = new FileEntry("10.jsonl", "file://test/1/10.jsonl");
        FileEntry legacyFile = new FileEntry(LEGACY_FILENAME, "file://test/1.jsonl");

        when(fileStorageService.getFileEntries(JOB_DIR + "/")).thenReturn(Set.of(listedTaskFile));
        when(fileStorageService.fileExists(JOB_DIR, "10.jsonl")).thenReturn(true);
        when(fileStorageService.getFileEntry(JOB_DIR, "10.jsonl")).thenReturn(resolvedTaskFile);
        when(fileStorageService.readFileToBytes(JOB_DIR, resolvedTaskFile))
            .thenReturn(jsonLine(logEntry(10L, "from task file")).getBytes(StandardCharsets.UTF_8));
        when(fileStorageService.fileExists(LEGACY_DIR, LEGACY_FILENAME)).thenReturn(true);
        when(fileStorageService.getFileEntry(LEGACY_DIR, LEGACY_FILENAME)).thenReturn(legacyFile);
        when(fileStorageService.readFileToBytes(LEGACY_DIR, legacyFile))
            .thenReturn(jsonLine(logEntry(10L, "from legacy file")).getBytes(StandardCharsets.UTF_8));

        List<LogEntry> logEntries = logFileStorage.readLogEntriesByJobId(JOB_ID);

        assertEquals(List.of("from task file", "from legacy file"), messagesOf(logEntries));

        verify(fileStorageService, never()).readFileToBytes(JOB_DIR, listedTaskFile);
    }

    @Test
    void testReadLogEntriesReadsTheTaskExecutionFileDirectly() {
        FileEntry taskFile = new FileEntry("10.jsonl", "file://test/1/10.jsonl");

        when(fileStorageService.fileExists(JOB_DIR, "10.jsonl")).thenReturn(true);
        when(fileStorageService.getFileEntry(JOB_DIR, "10.jsonl")).thenReturn(taskFile);
        when(fileStorageService.readFileToBytes(JOB_DIR, taskFile))
            .thenReturn(jsonLine(logEntry(10L, "direct")).getBytes(StandardCharsets.UTF_8));
        when(fileStorageService.fileExists(LEGACY_DIR, LEGACY_FILENAME)).thenReturn(false);

        List<LogEntry> logEntries = logFileStorage.readLogEntries(JOB_ID, 10L);

        assertEquals(List.of("direct"), messagesOf(logEntries));

        verify(fileStorageService, never()).getFileEntries(anyString());
    }

    @Test
    void testDeleteLogEntriesRemovesEachTaskFileAndTheLegacyJobFile() {
        FileEntry listedTaskFile = new FileEntry("/10.jsonl", "file://test/1/10.jsonl");
        FileEntry resolvedTaskFile = new FileEntry("10.jsonl", "file://test/1/10.jsonl");
        FileEntry legacyFile = new FileEntry(LEGACY_FILENAME, "file://test/1.jsonl");

        when(fileStorageService.getFileEntries(JOB_DIR + "/")).thenReturn(Set.of(listedTaskFile));
        when(fileStorageService.fileExists(JOB_DIR, "10.jsonl")).thenReturn(true);
        when(fileStorageService.getFileEntry(JOB_DIR, "10.jsonl")).thenReturn(resolvedTaskFile);
        when(fileStorageService.fileExists(LEGACY_DIR, LEGACY_FILENAME)).thenReturn(true);
        when(fileStorageService.getFileEntry(LEGACY_DIR, LEGACY_FILENAME)).thenReturn(legacyFile);

        logFileStorage.deleteLogEntries(JOB_ID);

        verify(fileStorageService).deleteFile(JOB_DIR, resolvedTaskFile);
        verify(fileStorageService).deleteFile(LEGACY_DIR, legacyFile);
    }

    @Test
    void testAJobDirectoryThatCannotBeListedCountsAsNoLogs() {
        when(fileStorageService.getFileEntries(JOB_DIR + "/")).thenThrow(new FileStorageException("no such directory"));
        when(fileStorageService.fileExists(LEGACY_DIR, LEGACY_FILENAME)).thenReturn(false);

        assertFalse(logFileStorage.logsExist(JOB_ID));
    }

    @Test
    void testAReadWaitsForThePendingWriteOfItsJob() {
        when(fileStorageService.fileExists(JOB_DIR, "10.jsonl")).thenReturn(false);
        when(fileStorageService.getFileEntries(JOB_DIR + "/")).thenReturn(Set.of());
        when(fileStorageService.fileExists(LEGACY_DIR, LEGACY_FILENAME)).thenReturn(false);

        logFileStorage.storeLogEntries(JOB_ID, 10L, List.of(logEntry(10L, "pending")));

        logFileStorage.logsExist(JOB_ID);

        verify(fileStorageService).storeFileContent(eq(JOB_DIR), eq("10.jsonl"), any(byte[].class), eq(false));
    }

    @Test
    void testReadLogEntriesKeepsOnlyTheTaskExecutionsEntriesFromTheLegacyJobFile() {
        FileEntry legacyFile = new FileEntry(LEGACY_FILENAME, "file://test/1.jsonl");
        String legacyContent = jsonLine(logEntry(10L, "mine")) + jsonLine(logEntry(20L, "someone else's")) +
            jsonLine(logEntry(10L, "mine too"));

        when(fileStorageService.fileExists(JOB_DIR, "10.jsonl")).thenReturn(false);
        when(fileStorageService.fileExists(LEGACY_DIR, LEGACY_FILENAME)).thenReturn(true);
        when(fileStorageService.getFileEntry(LEGACY_DIR, LEGACY_FILENAME)).thenReturn(legacyFile);
        when(fileStorageService.readFileToBytes(LEGACY_DIR, legacyFile))
            .thenReturn(legacyContent.getBytes(StandardCharsets.UTF_8));

        List<LogEntry> logEntries = logFileStorage.readLogEntries(JOB_ID, 10L);

        assertEquals(List.of("mine", "mine too"), messagesOf(logEntries));
    }

    @Test
    void testLogsExistWhenOnlyTheLegacyJobFileIsPresent() {
        when(fileStorageService.getFileEntries(JOB_DIR + "/")).thenReturn(Set.of());
        when(fileStorageService.fileExists(LEGACY_DIR, LEGACY_FILENAME)).thenReturn(true);

        assertTrue(logFileStorage.logsExist(JOB_ID));
    }

    @Test
    void testAnUnreadableLineIsSkippedWithoutHidingTheRest() {
        FileEntry taskFile = new FileEntry("10.jsonl", "file://test/1/10.jsonl");
        String content = jsonLine(logEntry(10L, "before")) + "{not json\n" + jsonLine(logEntry(10L, "after"));

        when(fileStorageService.fileExists(JOB_DIR, "10.jsonl")).thenReturn(true);
        when(fileStorageService.getFileEntry(JOB_DIR, "10.jsonl")).thenReturn(taskFile);
        when(fileStorageService.readFileToBytes(JOB_DIR, taskFile))
            .thenReturn(content.getBytes(StandardCharsets.UTF_8));
        when(fileStorageService.fileExists(LEGACY_DIR, LEGACY_FILENAME)).thenReturn(false);

        List<LogEntry> logEntries = logFileStorage.readLogEntries(JOB_ID, 10L);

        assertEquals(List.of("before", "after"), messagesOf(logEntries));
    }

    private static String jsonLine(LogEntry logEntry) {
        return JsonUtils.write(logEntry) + "\n";
    }

    private static LogEntry logEntry(long taskExecutionId, String message) {
        return LogEntry.builder()
            .timestamp(Instant.now())
            .level(LogEntry.Level.INFO)
            .componentName("httpClient")
            .taskExecutionId(taskExecutionId)
            .message(message)
            .build();
    }

    private static List<String> messagesOf(List<LogEntry> logEntries) {
        return logEntries.stream()
            .map(LogEntry::message)
            .toList();
    }
}
