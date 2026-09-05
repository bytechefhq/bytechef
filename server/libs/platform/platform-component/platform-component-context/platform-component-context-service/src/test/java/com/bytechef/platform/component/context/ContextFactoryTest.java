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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.file.storage.FileStorageServiceRegistry;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.platform.component.definition.LogEntryBufferAware;
import com.bytechef.platform.component.log.EditorLogFileStorage;
import com.bytechef.platform.component.log.LogFileStorage;
import com.bytechef.platform.component.log.TriggerLogFileStorage;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.file.storage.TempFileStorage;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Pins which log store each kind of context writes to, through the real {@link ContextFactoryImpl}.
 *
 * @author Ivica Cardic
 */
class ContextFactoryTest {

    private final EditorLogFileStorage editorLogFileStorage = mock(EditorLogFileStorage.class);
    private final LogFileStorage logFileStorage = mock(LogFileStorage.class);
    private final TriggerLogFileStorage triggerLogFileStorage = mock(TriggerLogFileStorage.class);

    private ContextFactory contextFactory;

    @BeforeEach
    void beforeEach() {
        ApplicationProperties applicationProperties = mock(ApplicationProperties.class);
        ApplicationProperties.FileStorage fileStorage = mock(ApplicationProperties.FileStorage.class);
        FileStorageServiceRegistry fileStorageServiceRegistry = mock(FileStorageServiceRegistry.class);

        when(applicationProperties.getFileStorage()).thenReturn(fileStorage);
        when(fileStorage.getProvider()).thenReturn(ApplicationProperties.FileStorage.Provider.values()[0]);
        when(fileStorageServiceRegistry.getFileStorageService(anyString())).thenReturn(mock(FileStorageService.class));

        contextFactory = new ContextFactoryImpl(
            mock(ApplicationContext.class), applicationProperties, mock(CacheManager.class), mock(DataStorage.class),
            editorLogFileStorage, mock(ApplicationEventPublisher.class), fileStorageServiceRegistry, logFileStorage,
            mock(TempFileStorage.class), mock(Tracer.class), triggerLogFileStorage);
    }

    @Test
    void testATriggerRunningForAnExecutionLogsToTheTriggerStoreUnderThatExecutionOnceFlushed() {
        TriggerContext triggerContext = contextFactory.createTriggerContext(
            "webhook", 1, "newRequest", null, "workflow-uuid", null, null, PlatformType.AUTOMATION, false, 77L);

        triggerContext.log(log -> log.info("request received"));

        verify(triggerLogFileStorage, never()).storeLogEntries(anyLong(), anyLong(), anyList());

        ((LogEntryBufferAware) triggerContext).flushLogEntries();

        verify(triggerLogFileStorage).storeLogEntries(eq(77L), eq(77L), anyList());
        verify(triggerLogFileStorage).awaitPendingWrites(77L, 77L);
        verify(logFileStorage, never()).storeLogEntries(anyLong(), anyLong(), anyList());
        verify(editorLogFileStorage, never()).storeLogEntries(anyLong(), anyLong(), anyList());
    }

    @Test
    void testATriggerWithoutAnExecutionLogsNowhere() {
        TriggerContext triggerContext = contextFactory.createTriggerContext(
            "webhook", 1, "newRequest", null, "workflow-uuid", null, null, PlatformType.AUTOMATION, true);

        triggerContext.log(log -> log.info("test output"));

        verify(triggerLogFileStorage, never()).storeLogEntries(anyLong(), anyLong(), anyList());
        verify(logFileStorage, never()).storeLogEntries(anyLong(), anyLong(), anyList());
        verify(editorLogFileStorage, never()).storeLogEntries(anyLong(), anyLong(), anyList());
    }

    @Test
    void testAnActionStillLogsToTheJobStoreOfItsEnvironment() {
        ActionContext productionContext = contextFactory.createActionContext(
            "httpClient", 1, "get", null, null, 1L, 10L, "workflowId", null, null, PlatformType.AUTOMATION, false);
        ActionContext editorContext = contextFactory.createActionContext(
            "httpClient", 1, "get", null, null, 2L, 20L, "workflowId", null, null, PlatformType.AUTOMATION, true);

        productionContext.log(log -> log.warn("production"));
        editorContext.log(log -> log.warn("editor"));

        verify(logFileStorage).storeLogEntries(eq(1L), eq(10L), anyList());
        verify(editorLogFileStorage).storeLogEntries(eq(2L), eq(20L), anyList());
        verify(triggerLogFileStorage, never()).storeLogEntries(anyLong(), anyLong(), any());
    }
}
