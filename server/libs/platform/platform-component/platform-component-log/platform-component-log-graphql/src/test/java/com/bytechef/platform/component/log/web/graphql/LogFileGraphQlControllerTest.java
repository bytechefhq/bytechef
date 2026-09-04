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

package com.bytechef.platform.component.log.web.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.platform.component.log.LogFileStorage;
import com.bytechef.platform.component.log.TriggerLogFileStorage;
import com.bytechef.platform.component.log.domain.LogEntry;
import com.bytechef.platform.component.log.web.graphql.LogFileGraphQlController.LogFilterInput;
import com.bytechef.platform.component.log.web.graphql.LogFileGraphQlController.LogPage;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class LogFileGraphQlControllerTest {

    private LogFileGraphQlController controller;
    private final LogFileStorage logFileStorage = mock(LogFileStorage.class);
    private final TriggerLogFileStorage triggerLogFileStorage = mock(TriggerLogFileStorage.class);

    @BeforeEach
    void beforeEach() {
        controller = new LogFileGraphQlController(logFileStorage, triggerLogFileStorage);
    }

    @Test
    void testTriggerExecutionFileLogsReadTheTriggerStoreSortedByTimeAndPaged() {
        Instant now = Instant.parse("2026-09-04T10:00:00Z");

        when(triggerLogFileStorage.readLogEntriesByJobId(77L)).thenReturn(List.of(
            logEntry(now.plusSeconds(2), LogEntry.Level.INFO, "third"),
            logEntry(now, LogEntry.Level.DEBUG, "first"),
            logEntry(now.plusSeconds(1), LogEntry.Level.WARN, "second")));

        LogPage page = controller.triggerExecutionFileLogs(77L, null, 0, 2);

        assertEquals(List.of("first", "second"), page.content()
            .stream()
            .map(LogEntry::message)
            .toList());
        assertEquals(3, page.totalElements());
        assertEquals(2, page.totalPages());
        assertTrue(page.hasNext());
        assertFalse(page.hasPrevious());

        verifyNoInteractions(logFileStorage);
    }

    @Test
    void testTriggerExecutionFileLogsApplyTheLevelFilter() {
        Instant now = Instant.parse("2026-09-04T10:00:00Z");

        when(triggerLogFileStorage.readLogEntriesByJobId(77L)).thenReturn(List.of(
            logEntry(now, LogEntry.Level.DEBUG, "noise"),
            logEntry(now.plusSeconds(1), LogEntry.Level.ERROR, "the one that matters")));

        LogPage page = controller.triggerExecutionFileLogs(
            77L, new LogFilterInput(LogEntry.Level.WARN, null, null, null, null, null), null, null);

        assertEquals(List.of("the one that matters"), page.content()
            .stream()
            .map(LogEntry::message)
            .toList());
    }

    @Test
    void testTriggerExecutionFileLogsExistAsksTheTriggerStore() {
        when(triggerLogFileStorage.logsExist(77L)).thenReturn(true);

        assertTrue(controller.triggerExecutionFileLogsExist(77L));

        verifyNoInteractions(logFileStorage);
    }

    @Test
    void testJobFileLogsStillReadTheJobStore() {
        when(logFileStorage.readLogEntriesByJobId(1L)).thenReturn(
            List.of(logEntry(Instant.parse("2026-09-04T10:00:00Z"), LogEntry.Level.INFO, "job entry")));

        LogPage page = controller.jobFileLogs(1L, null, null, null);

        assertEquals(List.of("job entry"), page.content()
            .stream()
            .map(LogEntry::message)
            .toList());

        verifyNoInteractions(triggerLogFileStorage);
    }

    private static LogEntry logEntry(Instant timestamp, LogEntry.Level level, String message) {
        return LogEntry.builder()
            .timestamp(timestamp)
            .level(level)
            .componentName("webhook")
            .taskExecutionId(77L)
            .triggerExecutionId(77L)
            .message(message)
            .build();
    }
}
