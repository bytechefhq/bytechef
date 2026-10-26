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

import com.bytechef.platform.component.log.domain.ComponentLogEntry;
import com.bytechef.platform.component.log.domain.LogEntry;
import com.bytechef.platform.component.log.repository.ComponentLogEntryRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;

/**
 * Stores log entries as rows of the {@code component_log_entry} table, for the {@code jdbc} file storage provider.
 *
 * @author Ivica Cardic
 */
public class JdbcLogFileStorage implements LogFileStorage {

    private static final Logger log = LoggerFactory.getLogger(JdbcLogFileStorage.class);

    private final ComponentLogEntryRepository componentLogEntryRepository;
    private final Scope scope;

    @SuppressFBWarnings("EI")
    public JdbcLogFileStorage(ComponentLogEntryRepository componentLogEntryRepository, Scope scope) {
        this.componentLogEntryRepository = componentLogEntryRepository;
        this.scope = scope;
    }

    @Override
    public void storeLogEntries(long jobId, long taskExecutionId, List<LogEntry> logEntries) {
        if (logEntries.isEmpty()) {
            return;
        }

        List<ComponentLogEntry> componentLogEntries = logEntries.stream()
            .map(logEntry -> new ComponentLogEntry(scope.ordinal(), jobId, taskExecutionId, logEntry))
            .toList();

        try {
            componentLogEntryRepository.saveAll(componentLogEntries);
        } catch (DataAccessException dataAccessException) {
            log.error(
                "Failed to store {} log entries for task execution {} of job {}", logEntries.size(), taskExecutionId,
                jobId, dataAccessException);
        }
    }

    @Override
    public List<LogEntry> readLogEntries(long jobId, long taskExecutionId) {
        return toLogEntries(
            componentLogEntryRepository.findAllByScopeAndJobIdAndTaskExecutionIdOrderById(
                scope.ordinal(), jobId, taskExecutionId));
    }

    @Override
    public List<LogEntry> readLogEntriesByJobId(long jobId) {
        return toLogEntries(componentLogEntryRepository.findAllByScopeAndJobIdOrderById(scope.ordinal(), jobId));
    }

    @Override
    public boolean logsExist(long jobId) {
        return componentLogEntryRepository.existsByScopeAndJobId(scope.ordinal(), jobId);
    }

    @Override
    public void deleteLogEntries(long jobId) {
        componentLogEntryRepository.deleteByScopeAndJobId(scope.ordinal(), jobId);
    }

    private static List<LogEntry> toLogEntries(List<ComponentLogEntry> componentLogEntries) {
        return componentLogEntries.stream()
            .map(ComponentLogEntry::toLogEntry)
            .toList();
    }

    /**
     * Separates the logs of production runs, editor test runs and trigger executions, whose ids come from different
     * sequences.
     */
    public enum Scope {
        JOB,
        EDITOR,
        TRIGGER
    }
}
