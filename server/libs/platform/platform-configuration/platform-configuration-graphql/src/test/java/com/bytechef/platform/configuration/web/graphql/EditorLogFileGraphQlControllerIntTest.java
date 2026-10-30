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

import static org.mockito.Mockito.when;

import com.bytechef.platform.component.log.EditorLogFileStorage;
import com.bytechef.platform.component.log.domain.LogEntry;
import com.bytechef.platform.configuration.web.graphql.config.PlatformConfigurationGraphQlConfigurationSharedMocks;
import com.bytechef.platform.configuration.web.graphql.config.PlatformConfigurationGraphQlTestConfiguration;
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
    PlatformConfigurationGraphQlTestConfiguration.class,
    EditorLogFileGraphQlController.class
})
@GraphQlTest(
    controllers = EditorLogFileGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@PlatformConfigurationGraphQlConfigurationSharedMocks
public class EditorLogFileGraphQlControllerIntTest {

    private static final String EDITOR_JOB_FILE_LOGS_DOCUMENT = """
        query EditorJobFileLogs($jobId: ID!, $filter: LogFilterInput, $page: Int, $size: Int) {
            editorJobFileLogs(jobId: $jobId, filter: $filter, page: $page, size: $size) {
                content {
                    message
                    clusterElementCallId
                }
                totalElements
            }
        }
        """;

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private EditorLogFileStorage editorLogFileStorage;

    @Test
    void testFiltersByClusterElementCallId() {
        Instant now = Instant.parse("2026-09-04T10:00:00Z");

        when(editorLogFileStorage.readLogEntriesByJobId(1L)).thenReturn(List.of(
            logEntry(now, "from a", "a"),
            logEntry(now.plusSeconds(1), "from b", "b"),
            logEntry(now.plusSeconds(2), "from nobody", null)));

        graphQlTester.document(EDITOR_JOB_FILE_LOGS_DOCUMENT)
            .variable("jobId", "1")
            .variable("filter", Map.of("clusterElementCallId", "a"))
            .execute()
            .path("editorJobFileLogs.content[*].message")
            .entityList(String.class)
            .containsExactly("from a")
            .path("editorJobFileLogs.content[0].clusterElementCallId")
            .entity(String.class)
            .isEqualTo("a")
            .path("editorJobFileLogs.totalElements")
            .entity(Integer.class)
            .isEqualTo(1);

        graphQlTester.document(EDITOR_JOB_FILE_LOGS_DOCUMENT)
            .variable("jobId", "1")
            .execute()
            .path("editorJobFileLogs.content[*].message")
            .entityList(String.class)
            .containsExactly("from a", "from b", "from nobody")
            .path("editorJobFileLogs.totalElements")
            .entity(Integer.class)
            .isEqualTo(3);
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
