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

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.platform.component.log.LogFileStorage;
import com.bytechef.platform.component.log.TriggerLogFileStorage;
import com.bytechef.platform.component.log.domain.LogEntry;
import com.bytechef.platform.component.log.web.graphql.config.PlatformComponentLogGraphQlTestConfiguration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    PlatformComponentLogGraphQlTestConfiguration.class,
    LogFileGraphQlController.class
})
@GraphQlTest(
    controllers = LogFileGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
public class LogFileGraphQlControllerIntTest {

    private static final String JOB_FILE_LOGS_DOCUMENT = """
        query JobFileLogs($jobId: ID!, $filter: LogFilterInput, $page: Int, $size: Int) {
            jobFileLogs(jobId: $jobId, filter: $filter, page: $page, size: $size) {
                content {
                    message
                    clusterElementCallId
                }
                totalElements
                totalPages
                pageNumber
                pageSize
                hasNext
                hasPrevious
            }
        }
        """;

    private static final String TRIGGER_EXECUTION_FILE_LOGS_DOCUMENT = """
        query TriggerExecutionFileLogs($triggerExecutionId: ID!, $filter: LogFilterInput, $page: Int, $size: Int) {
            triggerExecutionFileLogs(
                triggerExecutionId: $triggerExecutionId, filter: $filter, page: $page, size: $size) {

                content {
                    message
                    level
                }
                totalElements
                totalPages
                pageNumber
                pageSize
                hasNext
                hasPrevious
            }
        }
        """;

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private LogFileStorage logFileStorage;

    @MockitoBean
    private TriggerLogFileStorage triggerLogFileStorage;

    @Test
    void testTriggerExecutionFileLogsReadTheTriggerStoreSortedByTimeAndPaged() {
        Instant now = Instant.parse("2026-09-04T10:00:00Z");

        when(triggerLogFileStorage.readLogEntriesByJobId(77L)).thenReturn(List.of(
            logEntry(now.plusSeconds(2), LogEntry.Level.INFO, "third"),
            logEntry(now, LogEntry.Level.DEBUG, "first"),
            logEntry(now.plusSeconds(1), LogEntry.Level.WARN, "second")));

        graphQlTester.document(TRIGGER_EXECUTION_FILE_LOGS_DOCUMENT)
            .variable("triggerExecutionId", "77")
            .variable("page", 0)
            .variable("size", 2)
            .execute()
            .path("triggerExecutionFileLogs.content[*].message")
            .entityList(String.class)
            .containsExactly("first", "second")
            .path("triggerExecutionFileLogs.totalElements")
            .entity(Integer.class)
            .isEqualTo(3)
            .path("triggerExecutionFileLogs.totalPages")
            .entity(Integer.class)
            .isEqualTo(2)
            .path("triggerExecutionFileLogs.pageNumber")
            .entity(Integer.class)
            .isEqualTo(0)
            .path("triggerExecutionFileLogs.pageSize")
            .entity(Integer.class)
            .isEqualTo(2)
            .path("triggerExecutionFileLogs.hasNext")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("triggerExecutionFileLogs.hasPrevious")
            .entity(Boolean.class)
            .isEqualTo(false);

        verifyNoInteractions(logFileStorage);
    }

    @Test
    void testTriggerExecutionFileLogsApplyTheLevelFilter() {
        Instant now = Instant.parse("2026-09-04T10:00:00Z");

        when(triggerLogFileStorage.readLogEntriesByJobId(77L)).thenReturn(List.of(
            logEntry(now, LogEntry.Level.DEBUG, "noise"),
            logEntry(now.plusSeconds(1), LogEntry.Level.ERROR, "the one that matters")));

        graphQlTester.document(TRIGGER_EXECUTION_FILE_LOGS_DOCUMENT)
            .variable("triggerExecutionId", "77")
            .variable("filter", Map.of("minLevel", "WARN"))
            .execute()
            .path("triggerExecutionFileLogs.content[*].message")
            .entityList(String.class)
            .containsExactly("the one that matters")
            .path("triggerExecutionFileLogs.content[0].level")
            .entity(String.class)
            .isEqualTo("ERROR")
            .path("triggerExecutionFileLogs.totalElements")
            .entity(Integer.class)
            .isEqualTo(1);
    }

    @Test
    void testTriggerExecutionFileLogsExistAsksTheTriggerStore() {
        when(triggerLogFileStorage.logsExist(77L)).thenReturn(true);

        graphQlTester.document("""
            query {
                triggerExecutionFileLogsExist(triggerExecutionId: "77")
            }
            """)
            .execute()
            .path("triggerExecutionFileLogsExist")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(triggerLogFileStorage).logsExist(77L);
        verifyNoInteractions(logFileStorage);
    }

    @Test
    void testJobFileLogsStillReadTheJobStore() {
        when(logFileStorage.readLogEntriesByJobId(1L)).thenReturn(
            List.of(logEntry(Instant.parse("2026-09-04T10:00:00Z"), LogEntry.Level.INFO, "job entry")));

        graphQlTester.document(JOB_FILE_LOGS_DOCUMENT)
            .variable("jobId", "1")
            .execute()
            .path("jobFileLogs.content[*].message")
            .entityList(String.class)
            .containsExactly("job entry")
            .path("jobFileLogs.pageNumber")
            .entity(Integer.class)
            .isEqualTo(0)
            .path("jobFileLogs.pageSize")
            .entity(Integer.class)
            .isEqualTo(100);

        verifyNoInteractions(triggerLogFileStorage);
    }

    @Test
    void testFiltersByClusterElementCallId() {
        Instant now = Instant.parse("2026-09-04T10:00:00Z");

        when(logFileStorage.readLogEntriesByJobId(1L)).thenReturn(List.of(
            logEntry(now, LogEntry.Level.INFO, "from a", "a"),
            logEntry(now.plusSeconds(1), LogEntry.Level.INFO, "from b", "b"),
            logEntry(now.plusSeconds(2), LogEntry.Level.INFO, "from nobody", null)));

        graphQlTester.document(JOB_FILE_LOGS_DOCUMENT)
            .variable("jobId", "1")
            .variable("filter", Map.of("clusterElementCallId", "a"))
            .execute()
            .path("jobFileLogs.content[*].message")
            .entityList(String.class)
            .containsExactly("from a")
            .path("jobFileLogs.content[0].clusterElementCallId")
            .entity(String.class)
            .isEqualTo("a")
            .path("jobFileLogs.totalElements")
            .entity(Integer.class)
            .isEqualTo(1);

        graphQlTester.document(JOB_FILE_LOGS_DOCUMENT)
            .variable("jobId", "1")
            .execute()
            .path("jobFileLogs.content[*].message")
            .entityList(String.class)
            .containsExactly("from a", "from b", "from nobody")
            .path("jobFileLogs.totalElements")
            .entity(Integer.class)
            .isEqualTo(3);
    }

    @Test
    void testTaskExecutionFilterReadsOnlyThatTasksEntries() {
        Instant now = Instant.parse("2026-09-04T10:00:00Z");

        when(logFileStorage.readLogEntries(1L, 77L)).thenReturn(
            List.of(logEntry(now, LogEntry.Level.INFO, "task entry")));

        graphQlTester.document(JOB_FILE_LOGS_DOCUMENT)
            .variable("jobId", "1")
            .variable("filter", Map.of("taskExecutionId", "77"))
            .execute()
            .path("jobFileLogs.content[*].message")
            .entityList(String.class)
            .containsExactly("task entry");

        verify(logFileStorage, never()).readLogEntriesByJobId(1L);
    }

    @Test
    void testClusterElementCallFilterReadsOnlyThatCallsEntries() {
        Instant now = Instant.parse("2026-09-04T10:00:00Z");

        when(logFileStorage.readLogEntries(1L, 77L, "a")).thenReturn(
            List.of(logEntry(now, LogEntry.Level.INFO, "from a", "a")));

        graphQlTester.document(JOB_FILE_LOGS_DOCUMENT)
            .variable("jobId", "1")
            .variable("filter", Map.of("clusterElementCallId", "a", "taskExecutionId", "77"))
            .execute()
            .path("jobFileLogs.content[*].message")
            .entityList(String.class)
            .containsExactly("from a");

        verify(logFileStorage, never()).readLogEntriesByJobId(1L);
        verify(logFileStorage, never()).readLogEntries(1L, 77L);
    }

    private static LogEntry logEntry(Instant timestamp, LogEntry.Level level, String message) {
        return logEntry(timestamp, level, message, null);
    }

    private static LogEntry logEntry(
        Instant timestamp, LogEntry.Level level, String message, String clusterElementCallId) {

        return LogEntry.builder()
            .timestamp(timestamp)
            .level(level)
            .componentName("webhook")
            .taskExecutionId(77L)
            .triggerExecutionId(77L)
            .message(message)
            .clusterElementCallId(clusterElementCallId)
            .build();
    }
}
