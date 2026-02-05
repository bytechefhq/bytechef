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

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.platform.component.log.domain.LogEntry;
import com.bytechef.platform.component.log.web.graphql.LogFileGraphQlController.LogFilterInput;
import com.bytechef.platform.component.log.web.graphql.LogFileGraphQlController.LogPage;
import com.bytechef.platform.configuration.log.EditorLogFileStorageReader;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for querying and managing component execution logs used in editor/test environments.
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
public class EditorLogFileGraphQlController {

    private final EditorLogFileStorageReader editorLogFileStorageReader;

    @SuppressFBWarnings("EI")
    public EditorLogFileGraphQlController(EditorLogFileStorageReader editorLogFileStorageReader) {
        this.editorLogFileStorageReader = editorLogFileStorageReader;
    }

    @QueryMapping
    public LogPage editorJobFileLogs(
        @Argument long jobId,
        @Argument LogFilterInput filter,
        @Argument Integer page,
        @Argument Integer size) {

        List<LogEntry> allEntries = editorLogFileStorageReader.readLogEntriesByJobId(jobId);

        List<LogEntry> filteredEntries = applyFilters(allEntries, filter);

        filteredEntries = filteredEntries.stream()
            .sorted(Comparator.comparing(LogEntry::timestamp))
            .toList();

        int pageNumber = page != null ? page : 0;
        int pageSize = size != null ? size : 100;
        int start = pageNumber * pageSize;
        int end = Math.min(start + pageSize, filteredEntries.size());

        List<LogEntry> pageContent = start < filteredEntries.size()
            ? filteredEntries.subList(start, end)
            : List.of();

        int totalPages = (int) Math.ceil((double) filteredEntries.size() / pageSize);

        return new LogPage(
            pageContent,
            filteredEntries.size(),
            totalPages,
            pageNumber,
            pageSize,
            end < filteredEntries.size(),
            pageNumber > 0);
    }

    @QueryMapping
    public List<LogEntry> editorTaskExecutionFileLogs(@Argument long jobId, @Argument long taskExecutionId) {
        return editorLogFileStorageReader.readLogEntries(jobId, taskExecutionId);
    }

    @QueryMapping
    public boolean editorJobFileLogsExist(@Argument long jobId) {
        return editorLogFileStorageReader.logsExist(jobId);
    }

    private List<LogEntry> applyFilters(List<LogEntry> entries, LogFilterInput filter) {
        if (filter == null) {
            return entries;
        }

        return entries.stream()
            .filter(entry -> filter.minLevel() == null ||
                entry.level()
                    .isAtLeast(filter.minLevel()))
            .filter(entry -> filter.componentName() == null ||
                entry.componentName()
                    .equals(filter.componentName()))
            .filter(entry -> filter.taskExecutionId() == null ||
                entry.taskExecutionId() == filter.taskExecutionId())
            .filter(entry -> filter.fromTimestamp() == null ||
                !entry.timestamp()
                    .isBefore(Instant.parse(filter.fromTimestamp())))
            .filter(entry -> filter.toTimestamp() == null ||
                !entry.timestamp()
                    .isAfter(Instant.parse(filter.toTimestamp())))
            .filter(entry -> filter.searchText() == null ||
                entry.message()
                    .toLowerCase()
                    .contains(filter.searchText()
                        .toLowerCase()))
            .toList();
    }
}
