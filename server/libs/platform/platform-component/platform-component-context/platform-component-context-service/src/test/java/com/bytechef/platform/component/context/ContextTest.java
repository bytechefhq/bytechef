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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.bytechef.platform.component.log.LogFileStorageWriter;
import com.bytechef.platform.component.log.domain.LogEntry;
import com.bytechef.platform.file.storage.TempFileStorage;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.context.ApplicationContext;

/**
 * @author Ivica Cardic
 */
class ContextTest {

    private static final long JOB_ID = 42L;
    private static final long TASK_EXECUTION_ID = 4200L;

    private ContextImpl context;
    private LogFileStorageWriter logFileStorageWriter;

    @BeforeEach
    void beforeEach() {
        logFileStorageWriter = mock(LogFileStorageWriter.class);

        TempFileStorage tempFileStorage = mock(TempFileStorage.class);

        context = new ContextImpl(
            "httpClient", 1, "get", null, JOB_ID, TASK_EXECUTION_ID, true,
            new HttpClientExecutor(mock(ApplicationContext.class), tempFileStorage), tempFileStorage,
            logFileStorageWriter, true);
    }

    @Test
    void testEntriesAreHeldUntilFlushedAndWrittenAsOneBatchInOrder() {
        context.log(log -> log.trace("first"));
        context.log(log -> log.debug("second"));
        context.log(log -> log.info("third"));

        verify(logFileStorageWriter, never()).storeLogEntries(anyLong(), anyLong(), anyList());

        context.flushLogEntries();

        List<LogEntry> stored = capturedBatch();

        assertEquals(List.of("first", "second", "third"), stored.stream()
            .map(LogEntry::message)
            .toList());
        assertEquals(
            List.of(LogEntry.Level.TRACE, LogEntry.Level.DEBUG, LogEntry.Level.INFO), stored.stream()
                .map(LogEntry::level)
                .toList());
    }

    @Test
    void testEntriesLoggedAfterTheFlushAreWrittenImmediately() {
        context.log(log -> log.debug("during perform"));

        context.flushLogEntries();

        context.log(log -> log.debug("while the stream is consumed"));

        ArgumentCaptor<List<LogEntry>> batchArgumentCaptor = batchCaptor();

        verify(logFileStorageWriter, times(2)).storeLogEntries(
            eq(JOB_ID), eq(TASK_EXECUTION_ID), batchArgumentCaptor.capture());

        List<List<LogEntry>> batches = batchArgumentCaptor.getAllValues();

        assertEquals(List.of("during perform"), messagesOf(batches.get(0)));
        assertEquals(List.of("while the stream is consumed"), messagesOf(batches.get(1)));
    }

    @Test
    void testFlushingAnEmptyBufferWritesNothingButStillWaitsForDurability() {
        context.flushLogEntries();

        verify(logFileStorageWriter, never()).storeLogEntries(anyLong(), anyLong(), anyList());
        verify(logFileStorageWriter).awaitPendingWrites(JOB_ID, TASK_EXECUTION_ID);
    }

    @Test
    void testAFullBufferIsWrittenOutBeforeTheFlush() {
        for (int index = 0; index < 100; index++) {
            context.log(log -> log.debug("chatty"));
        }

        verify(logFileStorageWriter, times(1)).storeLogEntries(eq(JOB_ID), eq(TASK_EXECUTION_ID), anyList());

        context.log(log -> log.debug("one more"));

        context.flushLogEntries();

        ArgumentCaptor<List<LogEntry>> batchArgumentCaptor = batchCaptor();

        verify(logFileStorageWriter, times(2)).storeLogEntries(
            eq(JOB_ID), eq(TASK_EXECUTION_ID), batchArgumentCaptor.capture());

        List<List<LogEntry>> batches = batchArgumentCaptor.getAllValues();

        assertEquals(100, batches.get(0)
            .size());
        assertEquals(List.of("one more"), messagesOf(batches.get(1)));
    }

    @Test
    void testFlushingWaitsUntilTheJobsEntriesAreDurable() {
        context.log(log -> log.debug("done"));

        context.flushLogEntries();

        InOrder inOrder = inOrder(logFileStorageWriter);

        inOrder.verify(logFileStorageWriter)
            .storeLogEntries(eq(JOB_ID), eq(TASK_EXECUTION_ID), anyList());
        inOrder.verify(logFileStorageWriter)
            .awaitPendingWrites(JOB_ID, TASK_EXECUTION_ID);
    }

    @Test
    void testAWarningIsWrittenOutWithoutWaitingForTheFlush() {
        context.log(log -> log.debug("setting up"));
        context.log(log -> log.warn("something looks off"));

        ArgumentCaptor<List<LogEntry>> batchArgumentCaptor = batchCaptor();

        verify(logFileStorageWriter, times(1)).storeLogEntries(
            eq(JOB_ID), eq(TASK_EXECUTION_ID), batchArgumentCaptor.capture());

        assertEquals(List.of("setting up", "something looks off"), messagesOf(batchArgumentCaptor.getValue()));

        context.flushLogEntries();

        verify(logFileStorageWriter, times(1)).storeLogEntries(eq(JOB_ID), eq(TASK_EXECUTION_ID), anyList());
    }

    @Test
    void testAWriteThroughContextStoresEveryEntryAtOnceAndFlushingAddsNothing() {
        TempFileStorage tempFileStorage = mock(TempFileStorage.class);

        ContextImpl clusterElementContext = new ContextImpl(
            "openai", 1, "model", null, JOB_ID, TASK_EXECUTION_ID, true,
            new HttpClientExecutor(mock(ApplicationContext.class), tempFileStorage), tempFileStorage,
            logFileStorageWriter, false);

        clusterElementContext.log(log -> log.debug("prompt sent"));
        clusterElementContext.log(log -> log.debug("completion received"));

        ArgumentCaptor<List<LogEntry>> batchArgumentCaptor = batchCaptor();

        verify(logFileStorageWriter, times(2)).storeLogEntries(
            eq(JOB_ID), eq(TASK_EXECUTION_ID), batchArgumentCaptor.capture());

        List<List<LogEntry>> batches = batchArgumentCaptor.getAllValues();

        assertEquals(List.of("prompt sent"), messagesOf(batches.get(0)));
        assertEquals(List.of("completion received"), messagesOf(batches.get(1)));
        assertEquals("openai", batches.get(0)
            .get(0)
            .componentName());

        clusterElementContext.flushLogEntries();

        verify(logFileStorageWriter, times(2)).storeLogEntries(eq(JOB_ID), eq(TASK_EXECUTION_ID), anyList());
    }

    @Test
    void testAContextWithoutAJobStoresNothing() {
        TempFileStorage tempFileStorage = mock(TempFileStorage.class);

        ContextImpl jobLessContext = new ContextImpl(
            "httpClient", 1, "get", null, true,
            new HttpClientExecutor(mock(ApplicationContext.class), tempFileStorage), tempFileStorage);

        jobLessContext.log(log -> log.debug("dropped"));

        jobLessContext.flushLogEntries();

        verifyNoMoreInteractions(logFileStorageWriter);
    }

    private List<LogEntry> capturedBatch() {
        ArgumentCaptor<List<LogEntry>> batchArgumentCaptor = batchCaptor();

        verify(logFileStorageWriter, times(1)).storeLogEntries(
            eq(JOB_ID), eq(TASK_EXECUTION_ID), batchArgumentCaptor.capture());

        return batchArgumentCaptor.getValue();
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<List<LogEntry>> batchCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }

    private static List<String> messagesOf(List<LogEntry> logEntries) {
        return logEntries.stream()
            .map(LogEntry::message)
            .toList();
    }
}
