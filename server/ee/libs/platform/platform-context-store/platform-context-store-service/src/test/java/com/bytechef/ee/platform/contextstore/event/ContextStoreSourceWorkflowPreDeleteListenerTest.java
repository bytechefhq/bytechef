/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import com.bytechef.ee.platform.contextstore.service.ContextStoreSourceService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ContextStoreSourceWorkflowPreDeleteListenerTest {

    private static final String WORKFLOW_ID = "wf-1";

    private final ContextStoreSourceService contextStoreSourceService = mock(ContextStoreSourceService.class);
    private final ContextStoreSourceWorkflowPreDeleteListener listener =
        new ContextStoreSourceWorkflowPreDeleteListener(contextStoreSourceService);

    @Test
    void testThePointerIsClearedRatherThanTheSourceDeleted() {
        when(contextStoreSourceService.findAllByWorkflowId(WORKFLOW_ID)).thenReturn(List.of(source(11L)));

        listener.onWorkflowPreDelete(WORKFLOW_ID);

        ArgumentCaptor<ContextStoreSource> captor = ArgumentCaptor.forClass(ContextStoreSource.class);

        verify(contextStoreSourceService).update(captor.capture());

        assertThat(captor.getValue()
            .getWorkflowId()).isNull();

        // A source owns ingested content the workflow delete never asked to remove.
        verify(contextStoreSourceService, never()).delete(11L);
    }

    @Test
    void testEverySourcePointingAtTheWorkflowIsCleared() {
        when(contextStoreSourceService.findAllByWorkflowId(WORKFLOW_ID)).thenReturn(List.of(source(11L), source(12L)));

        listener.onWorkflowPreDelete(WORKFLOW_ID);

        verify(contextStoreSourceService, times(2)).update(any());
    }

    @Test
    void testAWorkflowWithNoSourcesWritesNothing() {
        when(contextStoreSourceService.findAllByWorkflowId(WORKFLOW_ID)).thenReturn(List.of());

        listener.onWorkflowPreDelete(WORKFLOW_ID);

        verify(contextStoreSourceService).findAllByWorkflowId(WORKFLOW_ID);
        verifyNoMoreInteractions(contextStoreSourceService);
    }

    private static ContextStoreSource source(long id) {
        ContextStoreSource contextStoreSource = new ContextStoreSource();

        contextStoreSource.setId(id);
        contextStoreSource.setWorkflowId(WORKFLOW_ID);

        return contextStoreSource;
    }
}
