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
import com.bytechef.file.storage.exception.FileStorageException;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.platform.component.log.domain.LogEntry;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.type.TypeReference;

/**
 * Persistent implementation of LogFileStorage that stores logs using FileStorageService in JSONL (JSON Lines) format.
 *
 * <p>
 * Each task execution owns its own file, {@code logs/component_execution/<jobId>/<taskExecutionId>.jsonl}, so two task
 * executions never rewrite the same file, not even from different workers. Writes run off the calling thread, under the
 * tenant of the caller, on a per-job chain that applies them in submission order, and every read of a job first waits
 * for that job's pending writes, so an entry is visible to a reader on this instance as soon as
 * {@link #storeLogEntries(long, long, List)} has returned.
 *
 * <p>
 * A job's files are listed with a trailing slash on the directory because prefix-based providers would otherwise let
 * job {@code 42} match jobs {@code 420}, {@code 4200} and so on. A listed entry is read back by its bare filename
 * against the directory without the slash, because a provider's listing URL is only resolvable against the directory
 * it was listed with. Jobs written before the per-task layout are still read from, and deleted along with, the legacy
 * {@code logs/component_execution/<jobId>.jsonl}.
 *
 * @author Ivica Cardic
 */
public class LogFileStorageImpl implements LogFileStorage {

    private static final String LOG_FILES_DIR = "logs/component_execution";

    private static final Logger log = LoggerFactory.getLogger(LogFileStorageImpl.class);

    private final ExecutorService asyncExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final FileStorageService fileStorageService;
    private final Map<Long, CompletableFuture<Void>> pendingWrites = new ConcurrentHashMap<>();

    @SuppressFBWarnings("EI")
    public LogFileStorageImpl(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public void storeLogEntries(long jobId, long taskExecutionId, List<LogEntry> logEntries) {
        if (logEntries.isEmpty()) {
            return;
        }

        List<LogEntry> logEntriesToStore = List.copyOf(logEntries);
        String tenantId = TenantContext.getCurrentTenantId();

        CompletableFuture<Void> pendingWrite = pendingWrites.compute(jobId, (key, previousWrite) -> {
            CompletableFuture<Void> previous = previousWrite == null
                ? CompletableFuture.completedFuture(null)
                : previousWrite;

            return previous
                .thenRunAsync(
                    () -> TenantContext.runWithTenantId(
                        tenantId, () -> appendLogEntries(jobId, taskExecutionId, logEntriesToStore)),
                    asyncExecutor)
                .exceptionally(throwable -> {
                    log.error(
                        "Failed to append {} log entries for task execution {} of job {}",
                        logEntriesToStore.size(), taskExecutionId, jobId, throwable);

                    return null;
                });
        });

        pendingWrite.whenComplete((result, throwable) -> pendingWrites.remove(jobId, pendingWrite));
    }

    @Override
    public List<LogEntry> readLogEntries(long jobId, long taskExecutionId) {
        awaitPendingWrites(jobId);

        List<LogEntry> logEntries = new ArrayList<>(
            readLogFile(getJobDirectory(jobId), getLogFilename(taskExecutionId)));

        for (LogEntry legacyLogEntry : readLogFile(LOG_FILES_DIR, getLogFilename(jobId))) {
            if (legacyLogEntry.taskExecutionId() == taskExecutionId) {
                logEntries.add(legacyLogEntry);
            }
        }

        return logEntries;
    }

    @Override
    public List<LogEntry> readLogEntriesByJobId(long jobId) {
        awaitPendingWrites(jobId);

        String jobDirectory = getJobDirectory(jobId);
        List<LogEntry> logEntries = new ArrayList<>();

        for (FileEntry fileEntry : listTaskLogFiles(jobDirectory)) {
            logEntries.addAll(readLogFile(jobDirectory, getBareFilename(fileEntry)));
        }

        logEntries.addAll(readLogFile(LOG_FILES_DIR, getLogFilename(jobId)));

        return logEntries;
    }

    @Override
    public boolean logsExist(long jobId) {
        awaitPendingWrites(jobId);

        Set<FileEntry> taskLogFiles = listTaskLogFiles(getJobDirectory(jobId));

        return !taskLogFiles.isEmpty() || fileStorageService.fileExists(LOG_FILES_DIR, getLogFilename(jobId));
    }

    @Override
    public void deleteLogEntries(long jobId) {
        awaitPendingWrites(jobId);

        String jobDirectory = getJobDirectory(jobId);

        for (FileEntry fileEntry : listTaskLogFiles(jobDirectory)) {
            deleteLogFile(jobDirectory, getBareFilename(fileEntry));
        }

        deleteLogFile(LOG_FILES_DIR, getLogFilename(jobId));
    }

    private void appendLogEntries(long jobId, long taskExecutionId, List<LogEntry> logEntries) {
        try {
            String jobDirectory = getJobDirectory(jobId);
            String filename = getLogFilename(taskExecutionId);
            StringBuilder logLinesBuilder = new StringBuilder();

            for (LogEntry logEntry : logEntries) {
                logLinesBuilder.append(JsonUtils.write(logEntry))
                    .append('\n');
            }

            byte[] logLinesBytes = logLinesBuilder.toString()
                .getBytes(StandardCharsets.UTF_8);

            if (fileStorageService.fileExists(jobDirectory, filename)) {
                FileEntry fileEntry = fileStorageService.getFileEntry(jobDirectory, filename);

                byte[] existingContent = fileStorageService.readFileToBytes(jobDirectory, fileEntry);

                byte[] newContent = new byte[existingContent.length + logLinesBytes.length];

                System.arraycopy(existingContent, 0, newContent, 0, existingContent.length);
                System.arraycopy(logLinesBytes, 0, newContent, existingContent.length, logLinesBytes.length);

                fileStorageService.storeFileContent(jobDirectory, filename, newContent, false);
            } else {
                fileStorageService.storeFileContent(jobDirectory, filename, logLinesBytes, false);
            }
        } catch (Exception exception) {
            log.error(
                "Failed to append {} log entries for task execution {} of job {}", logEntries.size(), taskExecutionId,
                jobId, exception);
        }
    }

    private void awaitPendingWrites(long jobId) {
        CompletableFuture<Void> pendingWrite = pendingWrites.get(jobId);

        if (pendingWrite != null) {
            pendingWrite.join();
        }
    }

    private void deleteLogFile(String directory, String filename) {
        if (fileStorageService.fileExists(directory, filename)) {
            FileEntry fileEntry = fileStorageService.getFileEntry(directory, filename);

            fileStorageService.deleteFile(directory, fileEntry);
        }
    }

    private static String getBareFilename(FileEntry fileEntry) {
        String filename = fileEntry.getName();

        return filename.startsWith("/") ? filename.substring(1) : filename;
    }

    private static String getJobDirectory(long jobId) {
        return LOG_FILES_DIR + "/" + jobId;
    }

    private static String getLogFilename(long id) {
        return id + ".jsonl";
    }

    private Set<FileEntry> listTaskLogFiles(String jobDirectory) {
        try {
            return fileStorageService.getFileEntries(jobDirectory + "/");
        } catch (FileStorageException exception) {
            return Set.of();
        }
    }

    private List<LogEntry> readLogFile(String directory, String filename) {
        if (!fileStorageService.fileExists(directory, filename)) {
            return List.of();
        }

        FileEntry fileEntry = fileStorageService.getFileEntry(directory, filename);

        String content = new String(fileStorageService.readFileToBytes(directory, fileEntry), StandardCharsets.UTF_8);

        return parseJsonLines(content);
    }

    private List<LogEntry> parseJsonLines(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        List<LogEntry> logEntries = new ArrayList<>();
        String[] lines = content.split("\n");

        for (String line : lines) {
            if (!line.isBlank()) {
                logEntries.add(JsonUtils.read(line, new TypeReference<>() {}));
            }
        }

        return logEntries;
    }
}
