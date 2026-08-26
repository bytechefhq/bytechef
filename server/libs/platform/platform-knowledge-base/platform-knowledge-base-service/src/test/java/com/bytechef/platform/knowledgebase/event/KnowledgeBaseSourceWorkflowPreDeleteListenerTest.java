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

package com.bytechef.platform.knowledgebase.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSource;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseSourceService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
class KnowledgeBaseSourceWorkflowPreDeleteListenerTest {

    private static final String WORKFLOW_ID = "wf-1";

    private final KnowledgeBaseSourceService knowledgeBaseSourceService = mock(KnowledgeBaseSourceService.class);
    private final KnowledgeBaseSourceWorkflowPreDeleteListener listener =
        new KnowledgeBaseSourceWorkflowPreDeleteListener(knowledgeBaseSourceService);

    @Test
    void testThePointerIsClearedRatherThanTheSourceDeleted() {
        KnowledgeBaseSource knowledgeBaseSource = source(11L);

        when(knowledgeBaseSourceService.findAllByWorkflowId(WORKFLOW_ID)).thenReturn(List.of(knowledgeBaseSource));

        listener.onWorkflowPreDelete(WORKFLOW_ID);

        ArgumentCaptor<KnowledgeBaseSource> captor = ArgumentCaptor.forClass(KnowledgeBaseSource.class);

        verify(knowledgeBaseSourceService).update(captor.capture());

        assertThat(captor.getValue()
            .getWorkflowId()).isNull();

        // A source owns ingested content the workflow delete never asked to remove.
        verify(knowledgeBaseSourceService, never()).delete(11L);
    }

    @Test
    void testEverySourcePointingAtTheWorkflowIsCleared() {
        when(knowledgeBaseSourceService.findAllByWorkflowId(WORKFLOW_ID))
            .thenReturn(List.of(source(11L), source(12L)));

        listener.onWorkflowPreDelete(WORKFLOW_ID);

        verify(knowledgeBaseSourceService, times(2)).update(any());
    }

    @Test
    void testAWorkflowWithNoSourcesWritesNothing() {
        when(knowledgeBaseSourceService.findAllByWorkflowId(WORKFLOW_ID)).thenReturn(List.of());

        listener.onWorkflowPreDelete(WORKFLOW_ID);

        verify(knowledgeBaseSourceService).findAllByWorkflowId(WORKFLOW_ID);
        verifyNoMoreInteractions(knowledgeBaseSourceService);
    }

    private static KnowledgeBaseSource source(long id) {
        KnowledgeBaseSource knowledgeBaseSource = new KnowledgeBaseSource();

        knowledgeBaseSource.setId(id);
        knowledgeBaseSource.setWorkflowId(WORKFLOW_ID);

        return knowledgeBaseSource;
    }
}
