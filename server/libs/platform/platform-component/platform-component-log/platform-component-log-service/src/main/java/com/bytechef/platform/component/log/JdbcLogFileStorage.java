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

import com.bytechef.platform.component.log.domain.LogEntry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

/**
 * Stores log entries as rows of the {@code component_log_entry} table, for the {@code jdbc} file storage provider.
 *
 * @author Ivica Cardic
 */
public class JdbcLogFileStorage implements LogFileStorage {

    private static final Logger log = LoggerFactory.getLogger(JdbcLogFileStorage.class);

    private static final String COLUMNS =
        "trigger_execution_id, task_execution_id, logged_at, level, component_name, " +
            "component_operation_name, message, exception_type, exception_message, stack_trace";
    private static final String DELETE = "DELETE FROM component_log_entry WHERE scope = :scope AND job_id = :jobId";
    private static final String EXISTS =
        "SELECT EXISTS (SELECT 1 FROM component_log_entry WHERE scope = :scope AND job_id = :jobId)";
    private static final String INSERT = "INSERT INTO component_log_entry (scope, job_id, " + COLUMNS + ") VALUES " +
        "(:scope, :jobId, :triggerExecutionId, :taskExecutionId, :loggedAt, :level, :componentName, " +
        ":componentOperationName, :message, :exceptionType, :exceptionMessage, :stackTrace)";
    private static final String SELECT_BY_JOB = "SELECT " + COLUMNS + " FROM component_log_entry " +
        "WHERE scope = :scope AND job_id = :jobId ORDER BY id";
    private static final String SELECT_BY_TASK_EXECUTION = "SELECT " + COLUMNS + " FROM component_log_entry " +
        "WHERE scope = :scope AND job_id = :jobId AND task_execution_id = :taskExecutionId ORDER BY id";

    private final NamedParameterJdbcOperations namedParameterJdbcOperations;
    private final Scope scope;

    @SuppressFBWarnings("EI")
    public JdbcLogFileStorage(NamedParameterJdbcOperations namedParameterJdbcOperations, Scope scope) {
        this.namedParameterJdbcOperations = namedParameterJdbcOperations;
        this.scope = scope;
    }

    @Override
    public void storeLogEntries(long jobId, long taskExecutionId, List<LogEntry> logEntries) {
        if (logEntries.isEmpty()) {
            return;
        }

        SqlParameterSource[] sqlParameterSources = logEntries.stream()
            .map(logEntry -> createSqlParameterSource(jobId, taskExecutionId, logEntry))
            .toArray(SqlParameterSource[]::new);

        try {
            namedParameterJdbcOperations.batchUpdate(INSERT, sqlParameterSources);
        } catch (DataAccessException dataAccessException) {
            log.error(
                "Failed to store {} log entries for task execution {} of job {}", logEntries.size(), taskExecutionId,
                jobId, dataAccessException);
        }
    }

    @Override
    public List<LogEntry> readLogEntries(long jobId, long taskExecutionId) {
        return namedParameterJdbcOperations.query(
            SELECT_BY_TASK_EXECUTION,
            Map.of("scope", scope.ordinal(), "jobId", jobId, "taskExecutionId", taskExecutionId),
            (resultSet, rowNum) -> toLogEntry(resultSet));
    }

    @Override
    public List<LogEntry> readLogEntriesByJobId(long jobId) {
        return namedParameterJdbcOperations.query(
            SELECT_BY_JOB, Map.of("scope", scope.ordinal(), "jobId", jobId),
            (resultSet, rowNum) -> toLogEntry(resultSet));
    }

    @Override
    public boolean logsExist(long jobId) {
        Boolean exists = namedParameterJdbcOperations.queryForObject(
            EXISTS, Map.of("scope", scope.ordinal(), "jobId", jobId), Boolean.class);

        return Boolean.TRUE.equals(exists);
    }

    @Override
    public void deleteLogEntries(long jobId) {
        namedParameterJdbcOperations.update(DELETE, Map.of("scope", scope.ordinal(), "jobId", jobId));
    }

    private SqlParameterSource createSqlParameterSource(long jobId, long taskExecutionId, LogEntry logEntry) {
        LogEntry.Level level = logEntry.level();

        return new MapSqlParameterSource()
            .addValue("scope", scope.ordinal())
            .addValue("jobId", jobId)
            .addValue("triggerExecutionId", logEntry.triggerExecutionId())
            .addValue("taskExecutionId", taskExecutionId)
            .addValue("loggedAt", Timestamp.from(logEntry.timestamp()))
            .addValue("level", level == null ? null : level.ordinal())
            .addValue("componentName", logEntry.componentName())
            .addValue("componentOperationName", logEntry.componentOperationName())
            .addValue("message", logEntry.message())
            .addValue("exceptionType", logEntry.exceptionType())
            .addValue("exceptionMessage", logEntry.exceptionMessage())
            .addValue("stackTrace", logEntry.stackTrace());
    }

    private static LogEntry toLogEntry(ResultSet resultSet) throws SQLException {
        Timestamp loggedAt = resultSet.getTimestamp("logged_at");

        return new LogEntry(
            loggedAt.toInstant(), toLevel(resultSet), resultSet.getString("component_name"),
            resultSet.getString("component_operation_name"), resultSet.getLong("task_execution_id"),
            resultSet.getObject("trigger_execution_id", Long.class), resultSet.getString("message"),
            resultSet.getString("exception_type"), resultSet.getString("exception_message"),
            resultSet.getString("stack_trace"));
    }

    private static LogEntry.@Nullable Level toLevel(ResultSet resultSet) throws SQLException {
        int level = resultSet.getInt("level");

        if (resultSet.wasNull()) {
            return null;
        }

        return LogEntry.Level.values()[level];
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
