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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.FileStorage.Provider;
import com.bytechef.file.storage.FileStorageServiceRegistry;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.exception.FileStorageException;
import com.bytechef.file.storage.filesystem.service.FilesystemFileStorageService;
import com.bytechef.platform.component.log.domain.LogEntry;
import com.bytechef.tenant.TenantContext;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class LogFileStorageImplIntTest {

    private static final long JOB_ID = 1L;
    private static final String JOB_DIR = "logs/component_execution/1";
    private static final String LEGACY_DIR = "logs/component_execution";
    private static final String LEGACY_FILENAME = "1.jsonl";

    @TempDir
    private Path baseDirectory;

    private AnnotationConfigApplicationContext applicationContext;
    private ControllableFileStorageService fileStorageService;
    private LogFileStorage logFileStorage;

    @BeforeEach
    void beforeEach() {
        fileStorageService = new ControllableFileStorageService(baseDirectory.toString());

        applicationContext = createApplicationContext(fileStorageService);

        logFileStorage = applicationContext.getBean(LogFileStorage.class);
    }

    @AfterEach
    void afterEach() {
        fileStorageService.releaseAllGates();

        applicationContext.close();

        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testTheContextWiresTheFileBackedLogFileStorage() {
        assertInstanceOf(LogFileStorageImpl.class, logFileStorage);
    }

    @Test
    void testStoreLogEntriesWritesEachTaskExecutionToItsOwnFile() throws IOException {
        logFileStorage.storeLogEntries(JOB_ID, 10L, List.of(logEntry(10L, "task ten")));
        logFileStorage.storeLogEntries(JOB_ID, 20L, List.of(logEntry(20L, "task twenty")));

        assertTrue(logFileStorage.logsExist(JOB_ID));

        String taskTenContent = readStoredFile("public", "10.jsonl");
        String taskTwentyContent = readStoredFile("public", "20.jsonl");

        assertTrue(taskTenContent.contains("task ten"));
        assertFalse(taskTenContent.contains("task twenty"));
        assertTrue(taskTwentyContent.contains("task twenty"));
        assertFalse(taskTwentyContent.contains("task ten"));
        assertEquals(List.of(), fileStorageService.getReadFiles());
    }

    @Test
    void testWritesRunOffThreadUnderTheCallersTenant() {
        TenantContext.setCurrentTenantId("000001");

        try {
            logFileStorage.storeLogEntries(JOB_ID, 10L, List.of(logEntry(10L, "tenant one")));

            logFileStorage.awaitPendingWrites(JOB_ID);
        } finally {
            TenantContext.resetCurrentTenantId();
        }

        Thread writeThread = fileStorageService.getWriteThread("10.jsonl");

        assertNotNull(writeThread);
        assertNotSame(Thread.currentThread(), writeThread);
        assertEquals("000001", fileStorageService.getWriteTenantId("10.jsonl"));
        assertTrue(Files.exists(getStoredFilePath("000001", "10.jsonl")));
        assertFalse(Files.exists(getStoredFilePath("public", "10.jsonl")));
    }

    @Test
    void testWritesForAJobAreAppliedInSubmissionOrder() throws IOException {
        List<String> expectedMessages = new ArrayList<>();

        for (int index = 0; index < 50; index++) {
            String message = "entry " + index;

            expectedMessages.add(message);

            logFileStorage.storeLogEntries(JOB_ID, 10L, List.of(logEntry(10L, message)));
        }

        assertEquals(expectedMessages, messagesOf(logFileStorage.readLogEntriesByJobId(JOB_ID)));

        String finalContent = readStoredFile("public", "10.jsonl");

        assertTrue(finalContent.indexOf("\"entry 0\"") < finalContent.indexOf("\"entry 49\""));
    }

    @Test
    void testAFailedWriteDoesNotStallLaterWritesForTheJob() throws IOException {
        fileStorageService.failNextExistenceCheck("10.jsonl");

        logFileStorage.storeLogEntries(JOB_ID, 10L, List.of(logEntry(10L, "first")));
        logFileStorage.storeLogEntries(JOB_ID, 10L, List.of(logEntry(10L, "second")));

        assertEquals(List.of("second"), messagesOf(logFileStorage.readLogEntriesByJobId(JOB_ID)));

        String content = readStoredFile("public", "10.jsonl");

        assertTrue(content.contains("second"));
        assertFalse(content.contains("first"));
    }

    @Test
    void testReadLogEntriesByJobIdCombinesTaskFilesWithTheLegacyJobFile() {
        storeFile(JOB_DIR, "10.jsonl", jsonLine(logEntry(10L, "from task file")));
        storeFile(LEGACY_DIR, LEGACY_FILENAME, jsonLine(logEntry(10L, "from legacy file")));

        List<LogEntry> logEntries = logFileStorage.readLogEntriesByJobId(JOB_ID);

        assertEquals(List.of("from task file", "from legacy file"), messagesOf(logEntries));
    }

    @Test
    void testAwaitingATaskExecutionDoesNotWaitForItsSiblings() {
        CountDownLatch siblingWriteLatch = fileStorageService.gateExistenceCheck("20.jsonl");

        logFileStorage.storeLogEntries(JOB_ID, 20L, List.of(logEntry(20L, "slow sibling")));
        logFileStorage.storeLogEntries(JOB_ID, 10L, List.of(logEntry(10L, "mine")));

        logFileStorage.awaitPendingWrites(JOB_ID, 10L);

        assertTrue(Files.exists(getStoredFilePath("public", "10.jsonl")));
        assertFalse(Files.exists(getStoredFilePath("public", "20.jsonl")));

        siblingWriteLatch.countDown();

        logFileStorage.awaitPendingWrites(JOB_ID);

        assertTrue(Files.exists(getStoredFilePath("public", "20.jsonl")));
    }

    @Test
    void testReadLogEntriesReadsTheTaskExecutionFileDirectly() {
        storeFile(JOB_DIR, "10.jsonl", jsonLine(logEntry(10L, "direct")));

        List<LogEntry> logEntries = logFileStorage.readLogEntries(JOB_ID, 10L);

        assertEquals(List.of("direct"), messagesOf(logEntries));
        assertEquals(List.of(), fileStorageService.getListedDirectories());
    }

    @Test
    void testDeleteLogEntriesRemovesEachTaskFileAndTheLegacyJobFile() {
        storeFile(JOB_DIR, "10.jsonl", jsonLine(logEntry(10L, "task file")));
        storeFile(LEGACY_DIR, LEGACY_FILENAME, jsonLine(logEntry(10L, "legacy file")));

        logFileStorage.deleteLogEntries(JOB_ID);

        assertFalse(Files.exists(getStoredFilePath("public", "10.jsonl")));
        assertFalse(Files.exists(getLegacyFilePath()));
        assertFalse(logFileStorage.logsExist(JOB_ID));
    }

    @Test
    void testAJobDirectoryThatCannotBeListedCountsAsNoLogs() throws IOException {
        Path storedFilePath = getStoredFilePath("public", "10.jsonl");

        Path jobDirectoryPath = Objects.requireNonNull(storedFilePath.getParent());

        Files.createDirectories(Objects.requireNonNull(jobDirectoryPath.getParent()));

        Files.writeString(jobDirectoryPath, "not a directory");

        assertFalse(logFileStorage.logsExist(JOB_ID));
    }

    @Test
    void testAReadWaitsForThePendingWriteOfItsJob() {
        CountDownLatch writeLatch = fileStorageService.gateExistenceCheck("10.jsonl");

        logFileStorage.storeLogEntries(JOB_ID, 10L, List.of(logEntry(10L, "pending")));

        assertFalse(Files.exists(getStoredFilePath("public", "10.jsonl")));

        Thread.ofVirtual()
            .start(() -> releaseAfterDelay(writeLatch));

        assertTrue(logFileStorage.logsExist(JOB_ID));
        assertTrue(Files.exists(getStoredFilePath("public", "10.jsonl")));
    }

    @Test
    void testAwaitPendingWritesReturnsOnceTheJobsWritesHaveLanded() throws IOException {
        CountDownLatch writeLatch = fileStorageService.gateExistenceCheck("10.jsonl");

        logFileStorage.storeLogEntries(JOB_ID, 10L, List.of(logEntry(10L, "must be durable")));

        Thread.ofVirtual()
            .start(() -> releaseAfterDelay(writeLatch));

        logFileStorage.awaitPendingWrites(JOB_ID);

        assertTrue(readStoredFile("public", "10.jsonl").contains("must be durable"));
    }

    @Test
    void testAwaitPendingWritesIsANoOpForAJobWithNothingQueued() {
        logFileStorage.awaitPendingWrites(JOB_ID);

        assertEquals(Map.of(), fileStorageService.getWriteThreads());
        assertFalse(Files.exists(getStoredFilePath("public", "10.jsonl").getParent()));
    }

    @Test
    void testReadLogEntriesKeepsOnlyTheTaskExecutionsEntriesFromTheLegacyJobFile() {
        String legacyContent = jsonLine(logEntry(10L, "mine")) + jsonLine(logEntry(20L, "someone else's")) +
            jsonLine(logEntry(10L, "mine too"));

        storeFile(LEGACY_DIR, LEGACY_FILENAME, legacyContent);

        List<LogEntry> logEntries = logFileStorage.readLogEntries(JOB_ID, 10L);

        assertEquals(List.of("mine", "mine too"), messagesOf(logEntries));
    }

    @Test
    void testLogsExistWhenOnlyTheLegacyJobFileIsPresent() {
        storeFile(LEGACY_DIR, LEGACY_FILENAME, jsonLine(logEntry(10L, "legacy only")));

        assertTrue(logFileStorage.logsExist(JOB_ID));
    }

    @Test
    void testALineWrittenBeforeTriggerExecutionIdExistedStillParses() {
        String legacyLine =
            "{\"timestamp\":\"2026-09-04T10:00:00Z\",\"level\":\"INFO\",\"componentName\":\"httpClient\","
                + "\"componentOperationName\":\"get\",\"taskExecutionId\":10,\"message\":\"from before\"}\n";

        storeFile(JOB_DIR, "10.jsonl", legacyLine);

        List<LogEntry> logEntries = logFileStorage.readLogEntries(JOB_ID, 10L);

        assertEquals(List.of("from before"), messagesOf(logEntries));

        LogEntry logEntry = logEntries.getFirst();

        assertNull(logEntry.triggerExecutionId());
    }

    @Test
    void testClusterElementCallFieldsSurviveWriteAndRead() {
        LogEntry stamped = LogEntry.builder()
            .timestamp(Instant.parse("2026-09-04T10:00:00Z"))
            .level(LogEntry.Level.INFO)
            .componentName("httpClient")
            .taskExecutionId(10L)
            .message("stamped")
            .clusterElementCallId("call-1")
            .clusterElementWorkflowNodeName("tool_1")
            .build();

        logFileStorage.storeLogEntries(JOB_ID, 10L, List.of(stamped));

        List<LogEntry> logEntries = logFileStorage.readLogEntries(JOB_ID, 10L);

        LogEntry logEntry = logEntries.getFirst();

        assertEquals("call-1", logEntry.clusterElementCallId());
        assertEquals("tool_1", logEntry.clusterElementWorkflowNodeName());
    }

    @Test
    void testAStoredLineWithoutClusterElementCallFieldsReadsWithBothNull() {
        String legacyLine =
            "{\"timestamp\":\"2026-09-04T10:00:00Z\",\"level\":\"INFO\",\"componentName\":\"httpClient\","
                + "\"taskExecutionId\":10,\"message\":\"from before\"}\n";

        storeFile(JOB_DIR, "10.jsonl", legacyLine);

        List<LogEntry> logEntries = logFileStorage.readLogEntries(JOB_ID, 10L);

        LogEntry logEntry = logEntries.getFirst();

        assertEquals("from before", logEntry.message());
        assertNull(logEntry.clusterElementCallId());
        assertNull(logEntry.clusterElementWorkflowNodeName());
    }

    @Test
    void testAnUnreadableLineIsSkippedWithoutHidingTheRest() {
        String content = jsonLine(logEntry(10L, "before")) + "{not json\n" + jsonLine(logEntry(10L, "after"));

        storeFile(JOB_DIR, "10.jsonl", content);

        List<LogEntry> logEntries = logFileStorage.readLogEntries(JOB_ID, 10L);

        assertEquals(List.of("before", "after"), messagesOf(logEntries));
    }

    private static AnnotationConfigApplicationContext createApplicationContext(
        ControllableFileStorageService fileStorageService) {

        ApplicationProperties applicationProperties = new ApplicationProperties();
        ApplicationProperties.FileStorage fileStorage = applicationProperties.getFileStorage();

        fileStorage.setProvider(Provider.FILESYSTEM);

        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();

        applicationContext.registerBean(ApplicationProperties.class, () -> applicationProperties);
        applicationContext.registerBean(
            FileStorageServiceRegistry.class, () -> new FileStorageServiceRegistry(List.of(fileStorageService)));
        applicationContext.scan("com.bytechef.platform.component.log.config");

        applicationContext.refresh();

        return applicationContext;
    }

    private Path getLegacyFilePath() {
        return baseDirectory.resolve(Path.of("public", LEGACY_DIR, LEGACY_FILENAME));
    }

    private Path getStoredFilePath(String tenantId, String filename) {
        return baseDirectory.resolve(Path.of(tenantId, JOB_DIR, filename));
    }

    private static String jsonLine(LogEntry logEntry) {
        return JsonUtils.write(logEntry) + "\n";
    }

    private static LogEntry logEntry(long taskExecutionId, String message) {
        return LogEntry.builder()
            .timestamp(Instant.now())
            .level(LogEntry.Level.INFO)
            .componentName("httpClient")
            .taskExecutionId(taskExecutionId)
            .message(message)
            .build();
    }

    private static List<String> messagesOf(List<LogEntry> logEntries) {
        return logEntries.stream()
            .map(LogEntry::message)
            .toList();
    }

    private String readStoredFile(String tenantId, String filename) throws IOException {
        return Files.readString(getStoredFilePath(tenantId, filename), StandardCharsets.UTF_8);
    }

    private static void releaseAfterDelay(CountDownLatch latch) {
        try {
            Thread.sleep(200);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread()
                .interrupt();
        } finally {
            latch.countDown();
        }
    }

    private void storeFile(String directory, String filename, String content) {
        fileStorageService.storeFileContent(directory, filename, content.getBytes(StandardCharsets.UTF_8), false);
    }

    private static class ControllableFileStorageService extends FilesystemFileStorageService {

        private final Set<String> failingFilenames = ConcurrentHashMap.newKeySet();
        private final Map<String, CountDownLatch> gatedFilenames = new ConcurrentHashMap<>();
        private final List<String> listedDirectories = new CopyOnWriteArrayList<>();
        private final List<String> readFiles = new CopyOnWriteArrayList<>();
        private final Map<String, String> writeTenantIds = new ConcurrentHashMap<>();
        private final Map<String, Thread> writeThreads = new ConcurrentHashMap<>();

        ControllableFileStorageService(String baseDirectory) {
            super(baseDirectory);
        }

        void failNextExistenceCheck(String filename) {
            failingFilenames.add(filename);
        }

        CountDownLatch gateExistenceCheck(String filename) {
            CountDownLatch latch = new CountDownLatch(1);

            gatedFilenames.put(filename, latch);

            return latch;
        }

        void releaseAllGates() {
            for (CountDownLatch latch : gatedFilenames.values()) {
                latch.countDown();
            }
        }

        List<String> getListedDirectories() {
            return List.copyOf(listedDirectories);
        }

        List<String> getReadFiles() {
            return List.copyOf(readFiles);
        }

        String getWriteTenantId(String filename) {
            return writeTenantIds.get(filename);
        }

        Thread getWriteThread(String filename) {
            return writeThreads.get(filename);
        }

        Map<String, Thread> getWriteThreads() {
            return Map.copyOf(writeThreads);
        }

        @Override
        public boolean fileExists(String directory, String filename) {
            CountDownLatch latch = gatedFilenames.get(filename);

            if (latch != null) {
                awaitLatch(latch);
            }

            if (failingFilenames.remove(filename)) {
                throw new FileStorageException("storage down");
            }

            return super.fileExists(directory, filename);
        }

        @Override
        public Set<FileEntry> getFileEntries(String directory) {
            listedDirectories.add(directory);

            return super.getFileEntries(directory);
        }

        @Override
        public byte[] readFileToBytes(String directory, FileEntry fileEntry) {
            readFiles.add(directory + "/" + fileEntry.getName());

            return super.readFileToBytes(directory, fileEntry);
        }

        @Override
        public FileEntry storeFileContent(String directory, String filename, byte[] data, boolean generateFilename) {
            writeTenantIds.put(filename, TenantContext.getCurrentTenantId());
            writeThreads.put(filename, Thread.currentThread());

            return super.storeFileContent(directory, filename, data, generateFilename);
        }

        private static void awaitLatch(CountDownLatch latch) {
            try {
                if (!latch.await(10, TimeUnit.SECONDS)) {
                    throw new FileStorageException("gate was not released");
                }
            } catch (InterruptedException interruptedException) {
                Thread.currentThread()
                    .interrupt();

                throw new FileStorageException("interrupted while gated");
            }
        }
    }
}
