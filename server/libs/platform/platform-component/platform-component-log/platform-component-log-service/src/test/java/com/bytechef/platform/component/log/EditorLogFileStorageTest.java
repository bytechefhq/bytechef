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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.file.storage.domain.FileEntry;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The editor store is {@link LogFileStorageImpl} rooted at {@code editor/logs}; these tests pin the root and the legacy
 * fallback there, the rest of the behaviour is covered by {@link LogFileStorageTest}.
 *
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class EditorLogFileStorageTest {

    private static final long JOB_ID = 7L;
    private static final String EDITOR_DIR = "editor/logs";
    private static final String EDITOR_JOB_DIR = "editor/logs/7";

    @Mock
    private FileStorageService fileStorageService;

    private EditorLogFileStorage editorLogFileStorage;

    @BeforeEach
    void beforeEach() {
        editorLogFileStorage = new EditorLogFileStorageImpl(fileStorageService);
    }

    @Test
    void testEntriesAreWrittenUnderTheEditorDirectory() {
        when(fileStorageService.fileExists(EDITOR_JOB_DIR, "70.jsonl")).thenReturn(false);
        when(fileStorageService.getFileEntries(EDITOR_JOB_DIR + "/")).thenReturn(Set.of());
        when(fileStorageService.fileExists(EDITOR_DIR, "7.jsonl")).thenReturn(false);

        editorLogFileStorage.storeLogEntries(JOB_ID, 70L, List.of(logEntry("editor run")));

        editorLogFileStorage.logsExist(JOB_ID);

        verify(fileStorageService).storeFileContent(eq(EDITOR_JOB_DIR), eq("70.jsonl"), any(byte[].class), eq(false));
    }

    @Test
    void testLegacyEditorJobFilesAreStillRead() {
        FileEntry legacyFile = new FileEntry("7.jsonl", "file://test/editor/7.jsonl");

        when(fileStorageService.getFileEntries(EDITOR_JOB_DIR + "/")).thenReturn(Set.of());
        when(fileStorageService.fileExists(EDITOR_DIR, "7.jsonl")).thenReturn(true);
        when(fileStorageService.getFileEntry(EDITOR_DIR, "7.jsonl")).thenReturn(legacyFile);
        when(fileStorageService.readFileToBytes(EDITOR_DIR, legacyFile))
            .thenReturn((JsonUtils.write(logEntry("from before")) + "\n").getBytes(StandardCharsets.UTF_8));

        List<LogEntry> logEntries = editorLogFileStorage.readLogEntriesByJobId(JOB_ID);

        assertEquals(List.of("from before"), logEntries.stream()
            .map(LogEntry::message)
            .toList());
    }

    private static LogEntry logEntry(String message) {
        return LogEntry.builder()
            .timestamp(Instant.now())
            .level(LogEntry.Level.DEBUG)
            .componentName("logger")
            .taskExecutionId(70L)
            .message(message)
            .build();
    }
}
