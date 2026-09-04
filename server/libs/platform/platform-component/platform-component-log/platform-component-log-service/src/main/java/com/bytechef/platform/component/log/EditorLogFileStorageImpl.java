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

import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.platform.component.log.domain.LogEntry;
import java.util.List;

/**
 * The editor store: a {@link LogFileStorageImpl} rooted at {@code editor/logs}, wrapped rather than subclassed so the
 * bean is an {@link EditorLogFileStorage} without also being a {@link LogFileStorage}. Editor test runs execute in a
 * single JVM, so the per-job write chain and its read barrier hold across the whole run.
 *
 * @author Ivica Cardic
 */
public class EditorLogFileStorageImpl implements EditorLogFileStorage {

    private static final String EDITOR_LOG_FILES_DIR = "editor/logs";

    private final LogFileStorageImpl logFileStorage;

    public EditorLogFileStorageImpl(FileStorageService fileStorageService) {
        this.logFileStorage = new LogFileStorageImpl(fileStorageService, EDITOR_LOG_FILES_DIR);
    }

    @Override
    public void awaitPendingWrites(long jobId) {
        logFileStorage.awaitPendingWrites(jobId);
    }

    @Override
    public void awaitPendingWrites(long jobId, long taskExecutionId) {
        logFileStorage.awaitPendingWrites(jobId, taskExecutionId);
    }

    @Override
    public void deleteLogEntries(long jobId) {
        logFileStorage.deleteLogEntries(jobId);
    }

    @Override
    public boolean logsExist(long jobId) {
        return logFileStorage.logsExist(jobId);
    }

    @Override
    public List<LogEntry> readLogEntries(long jobId, long taskExecutionId) {
        return logFileStorage.readLogEntries(jobId, taskExecutionId);
    }

    @Override
    public List<LogEntry> readLogEntriesByJobId(long jobId) {
        return logFileStorage.readLogEntriesByJobId(jobId);
    }

    @Override
    public void storeLogEntries(long jobId, long taskExecutionId, List<LogEntry> logEntries) {
        logFileStorage.storeLogEntries(jobId, taskExecutionId, logEntries);
    }
}
