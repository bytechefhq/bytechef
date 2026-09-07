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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.file.storage.filesystem.service.FilesystemFileStorageService;
import com.bytechef.platform.component.log.domain.LogEntry;
import com.bytechef.tenant.TenantContext;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class LogFileStorageTenantIsolationTest {

    private static final long JOB_ID = 1L;
    private static final long TASK_EXECUTION_ID = 10L;
    private static final String TENANT_ONE = "000001";
    private static final String TENANT_TWO = "000002";

    @TempDir
    private Path baseDirectory;

    private LogFileStorage logFileStorage;

    @BeforeEach
    void beforeEach() {
        logFileStorage = new LogFileStorageImpl(new FilesystemFileStorageService(baseDirectory.toString()));
    }

    @AfterEach
    void afterEach() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testEntriesAreStoredUnderTheStoringTenantsDirectory() {
        TenantContext.runWithTenantId(
            TENANT_ONE, () -> logFileStorage.storeLogEntries(JOB_ID, TASK_EXECUTION_ID, List.of(logEntry("one"))));

        logFileStorage.awaitPendingWrites(JOB_ID);

        assertTrue(Files.exists(baseDirectory.resolve("000001/logs/component_execution/1/10.jsonl")));
        assertFalse(Files.exists(baseDirectory.resolve("public/logs/component_execution/1/10.jsonl")));
    }

    @Test
    void testEntriesAreReadableByTheStoringTenantOnly() {
        TenantContext.runWithTenantId(
            TENANT_ONE, () -> logFileStorage.storeLogEntries(JOB_ID, TASK_EXECUTION_ID, List.of(logEntry("one"))));

        logFileStorage.awaitPendingWrites(JOB_ID);

        TenantContext.setCurrentTenantId(TENANT_ONE);

        assertTrue(logFileStorage.logsExist(JOB_ID));
        assertEquals(List.of("one"), messagesOf(logFileStorage.readLogEntriesByJobId(JOB_ID)));
        assertEquals(List.of("one"), messagesOf(logFileStorage.readLogEntries(JOB_ID, TASK_EXECUTION_ID)));

        TenantContext.setCurrentTenantId(TENANT_TWO);

        assertFalse(logFileStorage.logsExist(JOB_ID));
        assertEquals(List.of(), logFileStorage.readLogEntriesByJobId(JOB_ID));
        assertEquals(List.of(), logFileStorage.readLogEntries(JOB_ID, TASK_EXECUTION_ID));
    }

    @Test
    void testTenantsWithTheSameJobIdKeepSeparateFiles() {
        TenantContext.runWithTenantId(
            TENANT_ONE, () -> logFileStorage.storeLogEntries(JOB_ID, TASK_EXECUTION_ID, List.of(logEntry("one"))));
        TenantContext.runWithTenantId(
            TENANT_TWO, () -> logFileStorage.storeLogEntries(JOB_ID, TASK_EXECUTION_ID, List.of(logEntry("two"))));

        logFileStorage.awaitPendingWrites(JOB_ID);

        TenantContext.setCurrentTenantId(TENANT_ONE);

        assertEquals(List.of("one"), messagesOf(logFileStorage.readLogEntriesByJobId(JOB_ID)));

        TenantContext.setCurrentTenantId(TENANT_TWO);

        assertEquals(List.of("two"), messagesOf(logFileStorage.readLogEntriesByJobId(JOB_ID)));
    }

    private static LogEntry logEntry(String message) {
        return LogEntry.builder()
            .timestamp(Instant.now())
            .level(LogEntry.Level.INFO)
            .componentName("httpClient")
            .taskExecutionId(TASK_EXECUTION_ID)
            .message(message)
            .build();
    }

    private static List<String> messagesOf(List<LogEntry> logEntries) {
        return logEntries.stream()
            .map(LogEntry::message)
            .toList();
    }
}
