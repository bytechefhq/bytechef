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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.FileStorage.Provider;
import com.bytechef.file.storage.FileStorageServiceRegistry;
import com.bytechef.file.storage.filesystem.service.FilesystemFileStorageService;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRef;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallScope;
import com.bytechef.platform.component.log.LogFileStorage;
import com.bytechef.platform.component.log.LogFileStorageWriter;
import com.bytechef.platform.component.log.domain.LogEntry;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.bytechef.platform.file.storage.TempFileStorageImpl;
import com.bytechef.tenant.TenantContext;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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
class ContextImplIntTest {

    private static final long JOB_ID = 42L;
    private static final long TASK_EXECUTION_ID = 4200L;

    @TempDir
    private Path baseDirectory;

    private AnnotationConfigApplicationContext applicationContext;
    private ContextImpl context;
    private HttpClientExecutor httpClientExecutor;
    private LogFileStorage logFileStorage;
    private RecordingLogFileStorageWriter logFileStorageWriter;
    private TempFileStorage tempFileStorage;

    @BeforeEach
    void beforeEach() {
        applicationContext = createApplicationContext(baseDirectory);

        httpClientExecutor = applicationContext.getBean(HttpClientExecutor.class);
        logFileStorage = applicationContext.getBean(LogFileStorage.class);
        tempFileStorage = applicationContext.getBean(TempFileStorage.class);

        logFileStorageWriter = new RecordingLogFileStorageWriter(logFileStorage);

        context = new ContextImpl(
            "httpClient", 1, "get", null, JOB_ID, TASK_EXECUTION_ID, true, httpClientExecutor, tempFileStorage,
            logFileStorageWriter, true, null);
    }

    @AfterEach
    void afterEach() {
        applicationContext.close();

        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testEntriesAreHeldUntilFlushedAndWrittenAsOneBatchInOrder() {
        context.log(log -> log.trace("first"));
        context.log(log -> log.debug("second"));
        context.log(log -> log.info("third"));

        assertEquals(List.of(), logFileStorageWriter.getBatches());
        assertFalse(logFileStorage.logsExist(JOB_ID));

        context.flushLogEntries();

        List<LogEntry> stored = singleBatch();

        assertEquals(List.of("first", "second", "third"), messagesOf(stored));
        assertEquals(
            List.of(LogEntry.Level.TRACE, LogEntry.Level.DEBUG, LogEntry.Level.INFO), levelsOf(stored));

        List<LogEntry> readEntries = logFileStorage.readLogEntries(JOB_ID, TASK_EXECUTION_ID);

        assertEquals(List.of("first", "second", "third"), messagesOf(readEntries));
        assertEquals(
            List.of(LogEntry.Level.TRACE, LogEntry.Level.DEBUG, LogEntry.Level.INFO), levelsOf(readEntries));
    }

    @Test
    void testEntriesLoggedAfterTheFlushAreWrittenImmediately() {
        context.log(log -> log.debug("during perform"));

        context.flushLogEntries();

        context.log(log -> log.debug("while the stream is consumed"));

        List<List<LogEntry>> batches = logFileStorageWriter.getBatches();

        assertEquals(2, batches.size());
        assertEquals(List.of("during perform"), messagesOf(batches.get(0)));
        assertEquals(List.of("while the stream is consumed"), messagesOf(batches.get(1)));
        assertEquals(
            List.of("during perform", "while the stream is consumed"),
            messagesOf(logFileStorage.readLogEntries(JOB_ID, TASK_EXECUTION_ID)));
    }

    @Test
    void testFlushingAnEmptyBufferWritesNothingButStillWaitsForDurability() {
        context.flushLogEntries();

        assertEquals(List.of(), logFileStorageWriter.getBatches());
        assertEquals(List.of("await " + JOB_ID + "/" + TASK_EXECUTION_ID), logFileStorageWriter.getEvents());
        assertFalse(Files.exists(getLogFilePath("public", JOB_ID, TASK_EXECUTION_ID)));
    }

    @Test
    void testAFullBufferIsWrittenOutBeforeTheFlush() {
        for (int index = 0; index < 100; index++) {
            context.log(log -> log.debug("chatty"));
        }

        assertEquals(1, getBatchCount());

        context.log(log -> log.debug("one more"));

        context.flushLogEntries();

        List<List<LogEntry>> batches = logFileStorageWriter.getBatches();

        List<LogEntry> firstBatch = batches.get(0);
        List<LogEntry> storedLogEntries = logFileStorage.readLogEntries(JOB_ID, TASK_EXECUTION_ID);

        assertEquals(2, batches.size());
        assertEquals(100, firstBatch.size());
        assertEquals(List.of("one more"), messagesOf(batches.get(1)));
        assertEquals(101, storedLogEntries.size());
    }

    @Test
    void testFlushingWaitsUntilTheJobsEntriesAreDurable() throws IOException {
        context.log(log -> log.debug("done"));

        context.flushLogEntries();

        assertEquals(
            List.of("store " + JOB_ID + "/" + TASK_EXECUTION_ID, "await " + JOB_ID + "/" + TASK_EXECUTION_ID),
            logFileStorageWriter.getEvents());

        String fileContent = Files.readString(
            getLogFilePath("public", JOB_ID, TASK_EXECUTION_ID), StandardCharsets.UTF_8);

        assertTrue(fileContent.contains("\"done\""));
    }

    @Test
    void testAWarningIsWrittenOutWithoutWaitingForTheFlush() {
        context.log(log -> log.debug("setting up"));
        context.log(log -> log.warn("something looks off"));

        List<List<LogEntry>> batches = logFileStorageWriter.getBatches();

        assertEquals(1, batches.size());
        assertEquals(List.of("setting up", "something looks off"), messagesOf(batches.getFirst()));

        context.flushLogEntries();

        assertEquals(1, getBatchCount());
        assertEquals(
            List.of("setting up", "something looks off"),
            messagesOf(logFileStorage.readLogEntries(JOB_ID, TASK_EXECUTION_ID)));
    }

    @Test
    void testAWriteThroughContextStoresEveryEntryAtOnceAndFlushingAddsNothing() {
        ContextImpl clusterElementContext = new ContextImpl(
            "openai", 1, "model", null, JOB_ID, TASK_EXECUTION_ID, true, httpClientExecutor, tempFileStorage,
            logFileStorageWriter, false, null);

        clusterElementContext.log(log -> log.debug("prompt sent"));
        clusterElementContext.log(log -> log.debug("completion received"));

        List<List<LogEntry>> batches = logFileStorageWriter.getBatches();

        assertEquals(2, batches.size());
        assertEquals(List.of("prompt sent"), messagesOf(batches.get(0)));
        assertEquals(List.of("completion received"), messagesOf(batches.get(1)));

        List<LogEntry> firstBatch = batches.get(0);
        LogEntry firstLogEntry = firstBatch.getFirst();

        assertEquals("openai", firstLogEntry.componentName());

        clusterElementContext.flushLogEntries();

        assertEquals(2, getBatchCount());

        List<LogEntry> readEntries = logFileStorage.readLogEntries(JOB_ID, TASK_EXECUTION_ID);

        LogEntry firstReadEntry = readEntries.getFirst();

        assertEquals(List.of("prompt sent", "completion received"), messagesOf(readEntries));
        assertEquals("openai", firstReadEntry.componentName());
    }

    @Test
    void testATriggerContextStoresUnderItsTriggerExecutionInBothKeyPositions() {
        ContextImpl triggerContext = new ContextImpl(
            "webhook", 1, "newRequest", null, null, 0, false, httpClientExecutor, tempFileStorage,
            logFileStorageWriter, false, 77L);

        triggerContext.log(log -> log.info("request received"));

        assertEquals(List.of("store 77/77"), logFileStorageWriter.getEvents());

        triggerContext.flushLogEntries();

        assertEquals(List.of("store 77/77", "await 77/77"), logFileStorageWriter.getEvents());
        assertTrue(Files.exists(getLogFilePath("public", 77L, 77L)));

        List<LogEntry> readEntries = logFileStorage.readLogEntries(77L, 77L);

        assertEquals(1, readEntries.size());

        LogEntry logEntry = readEntries.getFirst();

        assertEquals(Long.valueOf(77L), logEntry.triggerExecutionId());
        assertEquals(77L, logEntry.taskExecutionId());
        assertEquals("request received", logEntry.message());
    }

    @Test
    void testAContextWithoutAJobStoresNothing() {
        ContextImpl jobLessContext = new ContextImpl(
            "httpClient", 1, "get", null, true, httpClientExecutor, tempFileStorage);

        jobLessContext.log(log -> log.debug("dropped"));

        jobLessContext.flushLogEntries();

        assertEquals(List.of(), logFileStorageWriter.getEvents());
        assertFalse(Files.exists(baseDirectory.resolve("public/logs")));
    }

    @Test
    void testAContextWithAWriterButNeitherAJobNorATriggerExecutionStoresNothing() {
        ContextImpl keyLessContext = new ContextImpl(
            "httpClient", 1, "get", null, null, 0, true, httpClientExecutor, tempFileStorage, logFileStorageWriter,
            true, null);

        keyLessContext.log(log -> log.warn("dropped"));

        keyLessContext.flushLogEntries();

        assertEquals(List.of(), logFileStorageWriter.getEvents());
        assertFalse(Files.exists(baseDirectory.resolve("public/logs")));
    }

    @Test
    void testEntriesLoggedFromAnotherThreadAreStoredUnderTheTenantThatCreatedTheContext() throws InterruptedException {
        ContextImpl tenantContext = TenantContext.callWithTenantId(
            "000001", () -> new ContextImpl(
                "aiAgent", 1, "streamChat", null, JOB_ID, TASK_EXECUTION_ID, false, httpClientExecutor,
                tempFileStorage, logFileStorageWriter, true, null));

        TenantContext.runWithTenantId("000001", () -> {
            tenantContext.log(log -> log.info("during perform"));

            tenantContext.flushLogEntries();
        });

        Thread streamThread = Thread.ofVirtual()
            .start(() -> tenantContext.log(log -> log.info("tool executed while the stream is consumed")));

        streamThread.join();

        assertEquals(List.of("000001", "000001"), logFileStorageWriter.getStoringTenantIds());

        List<LogEntry> readEntries = TenantContext.callWithTenantId(
            "000001", () -> logFileStorage.readLogEntries(JOB_ID, TASK_EXECUTION_ID));

        assertEquals(
            List.of("during perform", "tool executed while the stream is consumed"), messagesOf(readEntries));
        assertTrue(Files.exists(getLogFilePath("000001", JOB_ID, TASK_EXECUTION_ID)));
        assertFalse(Files.exists(getLogFilePath("public", JOB_ID, TASK_EXECUTION_ID)));
    }

    @Test
    void testStampsLogEntriesWithTheCurrentCall() {
        ClusterElementCallRef clusterElementCallRef = new ClusterElementCallRef("call-1", "tool_1");

        ClusterElementCallScope.callWith(clusterElementCallRef, () -> {
            context.log(log -> log.info("inside"));

            return null;
        });

        context.log(log -> log.info("outside"));
        context.flushLogEntries();

        assertStampedInsideOnly(singleBatch());
        assertStampedInsideOnly(logFileStorage.readLogEntries(JOB_ID, TASK_EXECUTION_ID));
    }

    private static void assertStampedInsideOnly(List<LogEntry> logEntries) {
        LogEntry insideLogEntry = logEntries.get(0);
        LogEntry outsideLogEntry = logEntries.get(1);

        assertEquals("inside", insideLogEntry.message());
        assertEquals("call-1", insideLogEntry.clusterElementCallId());
        assertEquals("tool_1", insideLogEntry.clusterElementWorkflowNodeName());
        assertEquals("outside", outsideLogEntry.message());
        assertNull(outsideLogEntry.clusterElementCallId());
        assertNull(outsideLogEntry.clusterElementWorkflowNodeName());
    }

    private static AnnotationConfigApplicationContext createApplicationContext(Path baseDirectory) {
        ApplicationProperties applicationProperties = new ApplicationProperties();
        ApplicationProperties.FileStorage fileStorage = applicationProperties.getFileStorage();

        fileStorage.setProvider(Provider.FILESYSTEM);

        FileStorageService fileStorageService = new FilesystemFileStorageService(baseDirectory.toString());

        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();

        applicationContext.registerBean(ApplicationProperties.class, () -> applicationProperties);
        applicationContext.registerBean(FileStorageService.class, () -> fileStorageService);
        applicationContext.registerBean(
            FileStorageServiceRegistry.class, () -> new FileStorageServiceRegistry(List.of(fileStorageService)));
        applicationContext.registerBean(TempFileStorage.class, () -> new TempFileStorageImpl(fileStorageService));
        applicationContext.registerBean(
            HttpClientExecutor.class,
            () -> new HttpClientExecutor(applicationContext, applicationContext.getBean(TempFileStorage.class)));
        applicationContext.scan("com.bytechef.platform.component.log.config");

        applicationContext.refresh();

        return applicationContext;
    }

    private Path getLogFilePath(String tenantId, long jobId, long taskExecutionId) {
        return baseDirectory.resolve(
            Path.of(tenantId, "logs", "component_execution", String.valueOf(jobId), taskExecutionId + ".jsonl"));
    }

    private int getBatchCount() {
        List<List<LogEntry>> batches = logFileStorageWriter.getBatches();

        return batches.size();
    }

    private static List<LogEntry.Level> levelsOf(List<LogEntry> logEntries) {
        return logEntries.stream()
            .map(LogEntry::level)
            .toList();
    }

    private static List<String> messagesOf(List<LogEntry> logEntries) {
        return logEntries.stream()
            .map(LogEntry::message)
            .toList();
    }

    private List<LogEntry> singleBatch() {
        List<List<LogEntry>> batches = logFileStorageWriter.getBatches();

        assertEquals(1, batches.size());

        return batches.getFirst();
    }

    private static class RecordingLogFileStorageWriter implements LogFileStorageWriter {

        private final List<List<LogEntry>> batches = new CopyOnWriteArrayList<>();
        private final LogFileStorageWriter delegate;
        private final List<String> events = new CopyOnWriteArrayList<>();
        private final List<String> storingTenantIds = new CopyOnWriteArrayList<>();

        RecordingLogFileStorageWriter(LogFileStorageWriter delegate) {
            this.delegate = delegate;
        }

        List<List<LogEntry>> getBatches() {
            return List.copyOf(batches);
        }

        List<String> getEvents() {
            return List.copyOf(events);
        }

        List<String> getStoringTenantIds() {
            return List.copyOf(storingTenantIds);
        }

        @Override
        public void storeLogEntries(long jobId, long taskExecutionId, List<LogEntry> logEntries) {
            batches.add(List.copyOf(logEntries));
            events.add("store " + jobId + "/" + taskExecutionId);
            storingTenantIds.add(TenantContext.getCurrentTenantId());

            delegate.storeLogEntries(jobId, taskExecutionId, logEntries);
        }

        @Override
        public void awaitPendingWrites(long jobId) {
            events.add("await " + jobId);

            delegate.awaitPendingWrites(jobId);
        }

        @Override
        public void awaitPendingWrites(long jobId, long taskExecutionId) {
            events.add("await " + jobId + "/" + taskExecutionId);

            delegate.awaitPendingWrites(jobId, taskExecutionId);
        }

        @Override
        public void deleteLogEntries(long jobId) {
            events.add("delete " + jobId);

            delegate.deleteLogEntries(jobId);
        }
    }
}
