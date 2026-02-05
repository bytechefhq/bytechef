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

/**
 * Interface for writing and managing storage of log entries associated with task executions. Provides methods to store
 * log records for specific task executions and to delete all logs related to a specific job for cleanup purposes.
 *
 * @author Ivica Cardic
 */
public interface LogFileStorageWriter {

    /**
     * Stores a log entry for a specific task execution.
     *
     * @param jobId           the job ID
     * @param taskExecutionId the task execution ID
     * @param logEntry        the log entry to store
     */
    void storeLogEntry(long jobId, long taskExecutionId, LogEntry logEntry);

    /**
     * Deletes logs for a job (cleanup).
     *
     * @param jobId the job ID
     */
    void deleteLogEntries(long jobId);
}
