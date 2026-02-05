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

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.platform.component.log.domain.LogEntry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import tools.jackson.core.type.TypeReference;

/**
 * Persistent implementation of LogFileStorage that stores logs using FileStorageService. Uses JSONL (JSON Lines) format
 * for efficient appending. Writes are performed asynchronously using virtual threads.
 *
 * @author Ivica Cardic
 */
public class LogFileStorageImpl implements LogFileStorage {

    private static final String LOG_FILES_DIR = "logs/component_execution";

    private final ExecutorService asyncExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final FileStorageService fileStorageService;

    @SuppressFBWarnings("EI")
    public LogFileStorageImpl(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public void storeLogEntry(long jobId, long taskExecutionId, LogEntry logEntry) {
        asyncExecutor.submit(() -> appendLogEntry(jobId, logEntry));
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
        String filename = jobId + ".jsonl";

        if (!fileStorageService.fileExists(LOG_FILES_DIR, filename)) {
            return List.of();
        }

        FileEntry fileEntry = fileStorageService.getFileEntry(LOG_FILES_DIR, filename);
        String content =
            new String(fileStorageService.readFileToBytes(LOG_FILES_DIR, fileEntry), StandardCharsets.UTF_8);

        return parseJsonLines(content);
    }

    @Override
    public boolean logsExist(long jobId) {
        return fileStorageService.fileExists(LOG_FILES_DIR, jobId + ".jsonl");
    }

    @Override
    public void deleteLogEntries(long jobId) {
        String filename = jobId + ".jsonl";

        if (fileStorageService.fileExists(LOG_FILES_DIR, filename)) {
            FileEntry fileEntry = fileStorageService.getFileEntry(LOG_FILES_DIR, filename);

            fileStorageService.deleteFile(LOG_FILES_DIR, fileEntry);
        }
    }

    private synchronized void appendLogEntry(long jobId, LogEntry logEntry) {
        String filename = jobId + ".jsonl";
        byte[] logLineBytes = (JsonUtils.write(logEntry) + "\n").getBytes(StandardCharsets.UTF_8);

        if (fileStorageService.fileExists(LOG_FILES_DIR, filename)) {
            FileEntry existingFile = fileStorageService.getFileEntry(LOG_FILES_DIR, filename);

            byte[] existingContent = fileStorageService.readFileToBytes(LOG_FILES_DIR, existingFile);

            byte[] newContent = new byte[existingContent.length + logLineBytes.length];

            System.arraycopy(existingContent, 0, newContent, 0, existingContent.length);
            System.arraycopy(logLineBytes, 0, newContent, existingContent.length, logLineBytes.length);

            fileStorageService.storeFileContent(LOG_FILES_DIR, filename, newContent);
        } else {
            fileStorageService.storeFileContent(LOG_FILES_DIR, filename, logLineBytes);
        }
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
