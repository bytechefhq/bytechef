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

package com.bytechef.platform.configuration.web.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.component.log.EditorLogFileStorage;
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
class EditorLogFileGraphQlControllerTest {

    private EditorLogFileGraphQlController controller;
    private final EditorLogFileStorage editorLogFileStorage = mock(EditorLogFileStorage.class);

    @BeforeEach
    void beforeEach() {
        controller = new EditorLogFileGraphQlController(editorLogFileStorage);
    }

    @Test
    void testFiltersByClusterElementCallId() {
        Instant now = Instant.parse("2026-09-04T10:00:00Z");

        when(editorLogFileStorage.readLogEntriesByJobId(1L)).thenReturn(List.of(
            logEntry(now, "from a", "a"),
            logEntry(now.plusSeconds(1), "from b", "b"),
            logEntry(now.plusSeconds(2), "from nobody", null)));

        LogPage filtered = controller.editorJobFileLogs(
            1L, new LogFilterInput(null, null, null, null, null, null, "a"), null, null);
        LogPage unfiltered = controller.editorJobFileLogs(1L, null, null, null);

        assertEquals(List.of("from a"), filtered.content()
            .stream()
            .map(LogEntry::message)
            .toList());
        assertEquals(3, unfiltered.totalElements());
    }

    private static LogEntry logEntry(Instant timestamp, String message, String clusterElementCallId) {
        return LogEntry.builder()
            .timestamp(timestamp)
            .level(LogEntry.Level.INFO)
            .componentName("agent")
            .taskExecutionId(5L)
            .message(message)
            .clusterElementCallId(clusterElementCallId)
            .build();
    }
}
