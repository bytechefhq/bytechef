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

package com.bytechef.platform.configuration.log;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.component.log.domain.LogEntry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;

/**
 * LogFileStorage implementation for reading logs in editor/test environments. Reads logs from temporary files using
 * JSONL format.
 *
 * @author Ivica Cardic
 */
@Component
class EditorLogFileStorageReaderImpl implements EditorLogFileStorageReader {

    private static final Logger logger = LoggerFactory.getLogger(EditorLogFileStorageReaderImpl.class);
    private static final String LOG_DIR_NAME = "bytechef_editor_logs";

    private final Path logDirectory;

    // Security: PATH_TRAVERSAL_IN is suppressed because logDirectory uses only a hardcoded constant (LOG_DIR_NAME)
    // combined with java.io.tmpdir system property, with no user input involved.
    // EI is suppressed because Path is effectively immutable and the field is private final.
    @SuppressFBWarnings(value = {
        "CT_CONSTRUCTOR_THROW", "EI", "PATH_TRAVERSAL_IN"
    }, justification = "Constructor must fail fast if log directory cannot be created; Path is immutable and private; "
        +
        "no user input in path construction")
    EditorLogFileStorageReaderImpl() {
        this.logDirectory = Path.of(System.getProperty("java.io.tmpdir"), LOG_DIR_NAME);

        try {
            Files.createDirectories(logDirectory);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to create editor log directory", exception);
        }
    }

    @Override
    public List<LogEntry> readLogEntries(long jobId, long taskExecutionId) {
        List<LogEntry> allEntries = readLogEntriesByJobId(jobId);

        return allEntries.stream()
            .filter(entry -> entry.taskExecutionId() == taskExecutionId)
            .toList();
    }

    @Override
    public List<LogEntry> readLogEntriesByJobId(long jobId) {
        Path logFile = getLogFilePath(jobId);

        if (!Files.exists(logFile)) {
            return List.of();
        }

        try {
            String content = Files.readString(logFile);

            return parseJsonLines(content);
        } catch (IOException exception) {
            logger.error("Failed to read log file: {}", logFile, exception);

            return List.of();
        }
    }

    @Override
    public boolean logsExist(long jobId) {
        Path logFile = getLogFilePath(jobId);

        try {
            return Files.exists(logFile) && Files.size(logFile) > 0;
        } catch (IOException exception) {
            return false;
        }
    }

    // Security: jobId is a primitive long, so it cannot contain path traversal characters
    private Path getLogFilePath(long jobId) {
        return logDirectory.resolve("job_" + jobId + ".jsonl");
    }

    private List<LogEntry> parseJsonLines(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        List<LogEntry> entries = new ArrayList<>();
        String[] lines = content.split("\n");

        for (String line : lines) {
            if (!line.isBlank()) {
                entries.add(JsonUtils.read(line, new TypeReference<>() {}));
            }
        }

        return entries;
    }
}
