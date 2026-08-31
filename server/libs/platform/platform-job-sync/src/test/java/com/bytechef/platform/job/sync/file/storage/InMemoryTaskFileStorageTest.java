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

package com.bytechef.platform.job.sync.file.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.file.storage.TaskFileStorageImpl;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class InMemoryTaskFileStorageTest {

    private static final long jobId = 123L;

    @Test
    void testStoreJobOutputsDelegatesToDurableStorage() {
        FileEntry durableFileEntry = new FileEntry("456.json", "base64://AAA=");
        Map<String, Object> outputs = Map.of("result", "ok");

        RecordingTaskFileStorage durable = new RecordingTaskFileStorage(durableFileEntry);

        InMemoryTaskFileStorage storage = new InMemoryTaskFileStorage(durable);

        FileEntry result = storage.storeJobOutputs(456L, outputs);

        assertSame(durableFileEntry, result);
        assertEquals(1, durable.storeJobOutputsCalls.get());
    }

    @Test
    void testReadJobOutputsCachesWriteSoDurableReadIsSkipped() {
        FileEntry durableFileEntry = new FileEntry("789.json", "base64://QkJC");
        Map<String, Object> outputs = Map.of("status", "done");

        RecordingTaskFileStorage durable = new RecordingTaskFileStorage(durableFileEntry);

        InMemoryTaskFileStorage storage = new InMemoryTaskFileStorage(durable);

        storage.storeJobOutputs(789L, outputs);

        Map<String, ?> readOnce = storage.readJobOutputs(durableFileEntry);
        Map<String, ?> readTwice = storage.readJobOutputs(durableFileEntry);

        assertEquals(outputs, readOnce);
        assertSame(readOnce, readTwice);
        assertEquals(0, durable.readJobOutputsCalls.get());
    }

    @Test
    void testReadJobOutputsMissFetchesFromDurableAndFillsCache() {
        FileEntry durableFileEntry = new FileEntry("999.json", "base64://Q0NE");
        Map<String, Object> outputs = Map.of("x", 1);

        RecordingTaskFileStorage durable = new RecordingTaskFileStorage(durableFileEntry, outputs);

        InMemoryTaskFileStorage storage = new InMemoryTaskFileStorage(durable);

        Map<String, ?> firstRead = storage.readJobOutputs(durableFileEntry);
        Map<String, ?> secondRead = storage.readJobOutputs(durableFileEntry);

        assertEquals(outputs, firstRead);
        assertSame(firstRead, secondRead);
        assertEquals(1, durable.readJobOutputsCalls.get());
    }

    @Test
    void testStoreContextValueDelegatesAndCaches() {
        FileEntry durableFileEntry = new FileEntry("ctx.json", "base64://Q1RY");
        Map<String, Object> contextValue = Map.of("key", "value");

        RecordingTaskFileStorage durable = new RecordingTaskFileStorage(durableFileEntry);

        InMemoryTaskFileStorage storage = new InMemoryTaskFileStorage(durable);

        FileEntry stored = storage.storeContextValue(jobId, Context.Classname.JOB, contextValue);

        assertSame(durableFileEntry, stored);

        Map<String, ?> read = storage.readContextValue(durableFileEntry);

        assertEquals(contextValue, read);
        assertEquals(1, durable.storeContextCalls.get());
        assertEquals(0, durable.readContextCalls.get());
    }

    @Test
    void testStoreTaskExecutionOutputDelegatesAndCaches() {
        FileEntry durableFileEntry = new FileEntry("42.json", "base64://VEVY");
        String output = "payload";

        RecordingTaskFileStorage durable = new RecordingTaskFileStorage(durableFileEntry);

        InMemoryTaskFileStorage storage = new InMemoryTaskFileStorage(durable);

        storage.storeTaskExecutionOutput(jobId, 42L, output);

        Object read = storage.readTaskExecutionOutput(durableFileEntry);

        assertSame(output, read);
        assertEquals(1, durable.storeTaskExecutionCalls.get());
        assertEquals(0, durable.readTaskExecutionCalls.get());
    }

    @Test
    void testCachedReadReturnsTheSameTypeAsTheDurableRead() {
        Object output = List.of(Map.of("APPLYDATE", Timestamp.from(Instant.parse("2026-08-26T00:00:00Z"))));

        TaskFileStorage taskFileStorage = new InMemoryTaskFileStorage(
            new TaskFileStorageImpl(new Base64FileStorageService()));

        FileEntry fileEntry = taskFileStorage.storeTaskExecutionOutput(1L, 1L, output);

        assertEquals(
            List.of(Map.of("APPLYDATE", ZonedDateTime.parse("2026-08-26T00:00:00Z"))),
            taskFileStorage.readTaskExecutionOutput(fileEntry));
    }

    private static final class RecordingTaskFileStorage implements TaskFileStorage {

        private final FileEntry storeResult;
        private final Object readResult;
        private final AtomicInteger storeJobOutputsCalls = new AtomicInteger();
        private final AtomicInteger readJobOutputsCalls = new AtomicInteger();
        private final AtomicInteger storeContextCalls = new AtomicInteger();
        private final AtomicInteger readContextCalls = new AtomicInteger();
        private final AtomicInteger storeTaskExecutionCalls = new AtomicInteger();
        private final AtomicInteger readTaskExecutionCalls = new AtomicInteger();

        RecordingTaskFileStorage(FileEntry storeResult) {
            this(storeResult, Map.of());
        }

        RecordingTaskFileStorage(FileEntry storeResult, Object readResult) {
            this.storeResult = storeResult;
            this.readResult = readResult;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Map<String, ?> readContextValue(FileEntry fileEntry) {
            readContextCalls.incrementAndGet();

            return (Map<String, ?>) readResult;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Map<String, ?> readJobOutputs(FileEntry fileEntry) {
            readJobOutputsCalls.incrementAndGet();

            return (Map<String, ?>) readResult;
        }

        @Override
        public Object readTaskExecutionOutput(FileEntry fileEntry) {
            readTaskExecutionCalls.incrementAndGet();

            return readResult;
        }

        @Override
        public FileEntry storeContextValue(long stackId, Context.Classname classname, Map<String, ?> value) {
            storeContextCalls.incrementAndGet();

            return storeResult;
        }

        @Override
        public FileEntry storeContextValue(
            long stackId, int subStackId, Context.Classname classname, Map<String, ?> value) {

            storeContextCalls.incrementAndGet();

            return storeResult;
        }

        @Override
        public FileEntry storeJobOutputs(long jobId, Map<String, ?> outputs) {
            storeJobOutputsCalls.incrementAndGet();

            return storeResult;
        }

        @Override
        public FileEntry storeTaskExecutionOutput(long jobId, long taskExecutionId, Object output) {
            storeTaskExecutionCalls.incrementAndGet();

            return storeResult;
        }
    }
}
