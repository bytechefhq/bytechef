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
 * The trigger store is {@link LogFileStorageImpl} rooted at {@code logs/trigger_execution}, keyed by trigger execution
 * in both positions; these tests pin the root, the rest of the behaviour is covered by {@link LogFileStorageTest}.
 *
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class TriggerLogFileStorageTest {

    private static final long JOB_ID = 7L;
    private static final String TRIGGER_DIR = "logs/trigger_execution";
    private static final String TRIGGER_EXECUTION_DIR = "logs/trigger_execution/7";

    @Mock
    private FileStorageService fileStorageService;

    private TriggerLogFileStorage triggerLogFileStorage;

    @BeforeEach
    void beforeEach() {
        triggerLogFileStorage = new TriggerLogFileStorageImpl(fileStorageService);
    }

    @Test
    void testEntriesAreWrittenUnderTheTriggerExecutionDirectory() {
        when(fileStorageService.fileExists(TRIGGER_EXECUTION_DIR, "7.jsonl")).thenReturn(false);
        when(fileStorageService.getFileEntries(TRIGGER_EXECUTION_DIR + "/")).thenReturn(Set.of());
        when(fileStorageService.fileExists(TRIGGER_DIR, "7.jsonl")).thenReturn(false);

        triggerLogFileStorage.storeLogEntries(JOB_ID, JOB_ID, List.of(logEntry("webhook received")));

        triggerLogFileStorage.logsExist(JOB_ID);

        verify(fileStorageService).storeFileContent(eq(TRIGGER_EXECUTION_DIR), eq("7.jsonl"), any(byte[].class),
            eq(false));
    }

    @Test
    void testALegacyFileUnderTheTriggerRootIsStillRead() {
        FileEntry legacyFile = new FileEntry("7.jsonl", "file://test/trigger/7.jsonl");

        when(fileStorageService.getFileEntries(TRIGGER_EXECUTION_DIR + "/")).thenReturn(Set.of());
        when(fileStorageService.fileExists(TRIGGER_DIR, "7.jsonl")).thenReturn(true);
        when(fileStorageService.getFileEntry(TRIGGER_DIR, "7.jsonl")).thenReturn(legacyFile);
        when(fileStorageService.readFileToBytes(TRIGGER_DIR, legacyFile))
            .thenReturn((JsonUtils.write(logEntry("from before")) + "\n").getBytes(StandardCharsets.UTF_8));

        List<LogEntry> logEntries = triggerLogFileStorage.readLogEntriesByJobId(JOB_ID);

        assertEquals(List.of("from before"), logEntries.stream()
            .map(LogEntry::message)
            .toList());
    }

    private static LogEntry logEntry(String message) {
        return LogEntry.builder()
            .timestamp(Instant.now())
            .level(LogEntry.Level.DEBUG)
            .componentName("logger")
            .taskExecutionId(7L)
            .triggerExecutionId(7L)
            .message(message)
            .build();
    }
}
