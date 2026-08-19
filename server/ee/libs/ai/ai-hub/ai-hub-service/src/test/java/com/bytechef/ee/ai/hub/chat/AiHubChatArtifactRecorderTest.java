/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Unit tests for {@link AiHubChatArtifactRecorderImpl}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AiHubChatArtifactRecorderTest {

    private static final String THREAD_ID = "thread-abc";
    private static final long USER_ID = 7L;

    @Mock
    private AiHubChatArtifactService chatArtifactService;

    private AiHubChatArtifactRecorderImpl recorder;
    private ListAppender<ILoggingEvent> logAppender;
    @SuppressWarnings("PMD")
    private Logger recorderLogger;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();

        @SuppressWarnings("unchecked")
        ObjectProvider<MeterRegistry> meterRegistryProvider = org.mockito.Mockito.mock(ObjectProvider.class);

        org.mockito.Mockito.when(meterRegistryProvider.getIfAvailable())
            .thenReturn(meterRegistry);

        recorder =
            new AiHubChatArtifactRecorderImpl(chatArtifactService, meterRegistryProvider);

        recorderLogger = (Logger) LoggerFactory.getLogger(AiHubChatArtifactRecorderImpl.class);
        logAppender = new ListAppender<>();

        logAppender.start();
        recorderLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        recorderLogger.detachAppender(logAppender);
    }

    @Test
    void testRecordDelegatesWhenUserIdProvided() {
        recorder.record(THREAD_ID, USER_ID, AiHubChatArtifactKind.WORKFLOW_CREATED.name(), "wf-1",
            "Workflow A");

        verify(chatArtifactService).record(
            eq(THREAD_ID), eq(USER_ID), eq(AiHubChatArtifactKind.WORKFLOW_CREATED),
            eq("wf-1"), eq("Workflow A"), any());
    }

    @Test
    void testRecordSkipsAndWarnsWhenUserIdMissing() {
        recorder.record(THREAD_ID, null, AiHubChatArtifactKind.WORKFLOW_CREATED.name(), "wf-1",
            "Workflow A");

        verify(chatArtifactService, never()).record(
            any(), anyLong(), any(), any(), any(), any());

        List<ILoggingEvent> warnEvents = logAppender.list.stream()
            .filter(event -> event.getLevel() == Level.WARN)
            .toList();

        assertThat(warnEvents).hasSize(1);

        String formattedMessage = warnEvents.get(0)
            .getFormattedMessage();

        assertThat(formattedMessage).contains(THREAD_ID);
        assertThat(formattedMessage).contains(AiHubChatArtifactKind.WORKFLOW_CREATED.name());
        assertThat(formattedMessage).contains("wf-1");
        assertThat(formattedMessage).contains("Workflow A");
    }

    @Test
    void testRecordIncrementsMissingUserIdCounterWhenUserIdMissing() {
        Counter counter = meterRegistry.find("bytechef.artifact.record.missing_userid_total")
            .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(0.0);

        recorder.record(THREAD_ID, null, AiHubChatArtifactKind.WORKFLOW_CREATED.name(), "wf-1",
            "Workflow A");

        assertThat(counter.count()).isEqualTo(1.0);

        recorder.record(THREAD_ID, null, AiHubChatArtifactKind.WORKFLOW_CREATED.name(), "wf-2",
            "Workflow B");

        assertThat(counter.count()).isEqualTo(2.0);
    }

    @Test
    void testRecordDoesNotIncrementCounterWhenUserIdProvided() {
        Counter counter = meterRegistry.find("bytechef.artifact.record.missing_userid_total")
            .counter();

        assertThat(counter).isNotNull();

        recorder.record(THREAD_ID, USER_ID, AiHubChatArtifactKind.WORKFLOW_CREATED.name(), "wf-1",
            "Workflow A");

        assertThat(counter.count()).isEqualTo(0.0);
    }
}
