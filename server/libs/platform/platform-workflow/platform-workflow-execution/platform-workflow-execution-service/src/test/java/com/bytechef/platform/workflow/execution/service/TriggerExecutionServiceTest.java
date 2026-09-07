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

package com.bytechef.platform.workflow.execution.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.execution.repository.TriggerExecutionRepository;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class TriggerExecutionServiceTest {

    @Disabled
    @Test
    public void testCreate() {
        // TODO
    }

    @Disabled
    @Test
    public void testGetTriggerExecution() {
        // TODO
    }

    @Disabled
    @Test
    public void testUpdate() {
        // TODO
    }

    @Test
    public void testGetTriggerExecutionsLoadsByIdAndSkipsTheRepositoryForNoIds() {
        TriggerExecutionRepository triggerExecutionRepository = mock(TriggerExecutionRepository.class);
        TriggerExecutionService triggerExecutionService = new TriggerExecutionServiceImpl(triggerExecutionRepository);

        TriggerExecution triggerExecution = TriggerExecution.builder()
            .id(77L)
            .build();

        when(triggerExecutionRepository.findAllById(List.of(77L))).thenReturn(List.of(triggerExecution));

        assertEquals(List.of(triggerExecution), triggerExecutionService.getTriggerExecutions(List.of(77L)));
        assertEquals(List.of(), triggerExecutionService.getTriggerExecutions(List.of()));

        verify(triggerExecutionRepository, never()).findAllById(List.of());
    }
}
