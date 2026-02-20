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
import com.bytechef.config.ApplicationProperties;
import com.bytechef.file.storage.FileStorageServiceRegistry;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.platform.component.log.domain.LogEntry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;

/**
 * LogFileStorage implementation for reading logs in editor/test environments. Reads logs from FileStorageService using
 * JSONL format.
 *
 * @author Ivica Cardic
 */
@Component
class EditorLogFileStorageReaderImpl implements EditorLogFileStorageReader {

    private static final String EDITOR_LOG_DIR = "editor/logs";

    private final FileStorageService fileStorageService;

    @SuppressFBWarnings("EI")
    EditorLogFileStorageReaderImpl(
        ApplicationProperties applicationProperties, FileStorageServiceRegistry fileStorageServiceRegistry) {

        this.fileStorageService = fileStorageServiceRegistry.getFileStorageService(
            applicationProperties.getFileStorage()
                .getProvider()
                .name());
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

        if (!fileStorageService.fileExists(EDITOR_LOG_DIR, filename)) {
            return List.of();
        }

        FileEntry fileEntry = fileStorageService.getFileEntry(EDITOR_LOG_DIR, filename);

        String content =
            new String(fileStorageService.readFileToBytes(EDITOR_LOG_DIR, fileEntry), StandardCharsets.UTF_8);

        return parseJsonLines(content);
    }

    @Override
    public boolean logsExist(long jobId) {
        return fileStorageService.fileExists(EDITOR_LOG_DIR, jobId + ".jsonl");
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
