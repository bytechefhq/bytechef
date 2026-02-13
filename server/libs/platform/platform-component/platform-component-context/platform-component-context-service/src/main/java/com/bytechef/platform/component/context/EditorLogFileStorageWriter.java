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

package com.bytechef.platform.component.context;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.component.log.LogFileStorageWriter;
import com.bytechef.platform.component.log.domain.LogEntry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LogFileStorage implementation for writing logs in editor/test environments. Writes logs to temporary files using
 * JSONL format asynchronously using virtual threads.
 *
 * @author Ivica Cardic
 */
public class EditorLogFileStorageWriter implements LogFileStorageWriter {

    private static final Logger logger = LoggerFactory.getLogger(EditorLogFileStorageWriter.class);

    private static final String LOG_DIR_NAME = "bytechef_editor_logs";

    private final ExecutorService asyncExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final Path logDirectory;

    // Security: PATH_TRAVERSAL_IN is suppressed because logDirectory uses only a hardcoded constant (LOG_DIR_NAME)
    // combined with java.io.tmpdir system property, with no user input involved.
    // EI is suppressed because Path is effectively immutable and the field is private final.
    @SuppressFBWarnings(value = {
        "CT_CONSTRUCTOR_THROW", "EI", "PATH_TRAVERSAL_IN"
    }, justification = "Constructor must fail fast if log directory cannot be created; Path is immutable and private; "
        +
        "no user input in path construction")
    public EditorLogFileStorageWriter() {
        this.logDirectory = Path.of(System.getProperty("java.io.tmpdir"), LOG_DIR_NAME);

        try {
            Files.createDirectories(logDirectory);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to create editor log directory", exception);
        }
    }

    @Override
    public void storeLogEntry(long jobId, long taskExecutionId, LogEntry logEntry) {
        asyncExecutor.submit(() -> appendLogEntry(jobId, logEntry));
    }

    @Override
    public void deleteLogEntries(long jobId) {
        Path logFile = getLogFilePath(jobId);

        try {
            Files.deleteIfExists(logFile);
        } catch (IOException exception) {
            logger.warn("Failed to delete editor log file: {}", logFile, exception);
        }
    }

    // Security: jobId is a primitive long, so it cannot contain path traversal characters
    private Path getLogFilePath(long jobId) {
        return logDirectory.resolve("job_" + jobId + ".jsonl");
    }

    private synchronized void appendLogEntry(long jobId, LogEntry logEntry) {
        try {
            Path logFile = getLogFilePath(jobId);

            if (!Files.exists(logFile)) {
                Files.createFile(logFile);
            }

            String logLine = JsonUtils.write(logEntry) + "\n";

            Files.writeString(logFile, logLine, StandardOpenOption.APPEND);
        } catch (IOException exception) {
            logger.error("Failed to append log entry for job {}", jobId, exception);
        }
    }
}
