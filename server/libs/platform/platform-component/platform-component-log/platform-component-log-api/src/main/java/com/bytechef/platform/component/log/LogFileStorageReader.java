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
import java.util.List;

/**
 * Interface for reading log entries from a log file storage system. Provides methods to retrieve logs for specific task
 * executions, entire jobs, and to check the existence of logs for a job.
 *
 * @author Ivica Cardic
 */
public interface LogFileStorageReader {

    /**
     * Reads all log entries for a specific task execution.
     *
     * @param jobId           the job ID
     * @param taskExecutionId the task execution ID
     * @return list of log entries for the task execution
     */
    List<LogEntry> readLogEntries(long jobId, long taskExecutionId);

    /**
     * Reads all log entries for an entire job (workflow execution).
     *
     * @param jobId the job ID
     * @return list of all log entries for the job
     */
    List<LogEntry> readLogEntriesByJobId(long jobId);

    /**
     * Checks if logs exist for a specific job.
     *
     * @param jobId the job ID
     * @return true if logs exist, false otherwise
     */
    boolean logsExist(long jobId);
}
