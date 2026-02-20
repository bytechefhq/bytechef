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
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.platform.component.log.LogFileStorageWriter;
import com.bytechef.platform.component.log.domain.LogEntry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LogFileStorage implementation for writing logs in editor/test environments. Writes logs using FileStorageService in
 * JSONL format asynchronously using virtual threads.
 *
 * @author Ivica Cardic
 */
public class EditorLogFileStorageWriter implements LogFileStorageWriter {

    private static final String EDITOR_LOG_DIR = "editor/logs";

    private static final Logger logger = LoggerFactory.getLogger(EditorLogFileStorageWriter.class);

    private final ExecutorService asyncExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final FileStorageService fileStorageService;

    @SuppressFBWarnings("EI")
    public EditorLogFileStorageWriter(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public void storeLogEntry(long jobId, long taskExecutionId, LogEntry logEntry) {
        asyncExecutor.submit(() -> appendLogEntry(jobId, logEntry));
    }

    @Override
    public void deleteLogEntries(long jobId) {
        try {
            String filename = jobId + ".jsonl";

            if (fileStorageService.fileExists(EDITOR_LOG_DIR, filename)) {
                FileEntry fileEntry = fileStorageService.getFileEntry(EDITOR_LOG_DIR, filename);

                fileStorageService.deleteFile(EDITOR_LOG_DIR, fileEntry);
            }
        } catch (Exception exception) {
            logger.warn("Failed to delete editor log entries for job {}", jobId, exception);
        }
    }

    private synchronized void appendLogEntry(long jobId, LogEntry logEntry) {
        try {
            String filename = jobId + ".jsonl";
            byte[] logLineBytes = (JsonUtils.write(logEntry) + "\n").getBytes(StandardCharsets.UTF_8);

            if (fileStorageService.fileExists(EDITOR_LOG_DIR, filename)) {
                FileEntry existingFile = fileStorageService.getFileEntry(EDITOR_LOG_DIR, filename);

                byte[] existingContent = fileStorageService.readFileToBytes(EDITOR_LOG_DIR, existingFile);

                byte[] newContent = new byte[existingContent.length + logLineBytes.length];

                System.arraycopy(existingContent, 0, newContent, 0, existingContent.length);
                System.arraycopy(logLineBytes, 0, newContent, existingContent.length, logLineBytes.length);

                fileStorageService.storeFileContent(EDITOR_LOG_DIR, filename, newContent, false);
            } else {
                fileStorageService.storeFileContent(EDITOR_LOG_DIR, filename, logLineBytes, false);
            }
        } catch (Exception exception) {
            logger.error("Failed to append log entry for job {}", jobId, exception);
        }
    }
}
