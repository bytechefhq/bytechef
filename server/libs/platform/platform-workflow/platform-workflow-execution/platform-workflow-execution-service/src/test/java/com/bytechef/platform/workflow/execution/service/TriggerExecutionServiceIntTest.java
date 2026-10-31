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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.config.PlatformWorkflowExecutionIntTestConfiguration;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.execution.repository.TriggerExecutionRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = PlatformWorkflowExecutionIntTestConfiguration.class)
class TriggerExecutionServiceIntTest {

    private static final Instant START_DATE = Instant.parse("2026-03-10T12:00:00Z")
        .truncatedTo(ChronoUnit.MILLIS);

    @MockitoSpyBean
    private TriggerExecutionRepository triggerExecutionRepository;

    @Autowired
    private TriggerExecutionService triggerExecutionService;

    @AfterEach
    void afterEach() {
        triggerExecutionRepository.deleteAll();
    }

    @Test
    void testCreate() {
        TriggerExecution triggerExecution = triggerExecutionService.create(
            getTriggerExecution("trigger-a", TriggerExecution.Status.CREATED, null));

        assertThat(triggerExecution.getId()).isNotNull();

        Optional<TriggerExecution> savedTriggerExecutionOptional = triggerExecutionRepository.findById(
            triggerExecution.getId());

        assertThat(savedTriggerExecutionOptional).hasValueSatisfying(savedTriggerExecution -> {
            assertThat(savedTriggerExecution.getStatus()).isEqualTo(TriggerExecution.Status.CREATED);
            assertThat(savedTriggerExecution.getCreatedBy()).isEqualTo("system");
            assertThat(savedTriggerExecution.getWorkflowExecutionId()
                .getTriggerName()).isEqualTo("trigger-a");
        });

        TriggerExecution existingTriggerExecution = getTriggerExecution(
            "trigger-a", TriggerExecution.Status.CREATED, null);

        existingTriggerExecution.setId(triggerExecution.getId());

        assertThatThrownBy(() -> triggerExecutionService.create(existingTriggerExecution))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testGetTriggerExecution() {
        TriggerExecution triggerExecution = triggerExecutionService.create(
            getTriggerExecution("trigger-a", TriggerExecution.Status.STARTED, START_DATE));

        TriggerExecution loadedTriggerExecution = triggerExecutionService.getTriggerExecution(
            triggerExecution.getId());

        assertThat(loadedTriggerExecution.getId()).isEqualTo(triggerExecution.getId());
        assertThat(loadedTriggerExecution.getStatus()).isEqualTo(TriggerExecution.Status.STARTED);
        assertThat(loadedTriggerExecution.getStartDate()).isEqualTo(START_DATE);
        assertThat(loadedTriggerExecution.getWorkflowTrigger()
            .getName()).isEqualTo("trigger");
    }

    @Test
    void testUpdate() {
        TriggerExecution triggerExecution = triggerExecutionService.create(
            getTriggerExecution("trigger-a", TriggerExecution.Status.STARTED, START_DATE));

        TriggerExecution completedTriggerExecution = triggerExecutionService.getTriggerExecution(
            triggerExecution.getId());

        completedTriggerExecution.setStartDate(null);
        completedTriggerExecution.setStatus(TriggerExecution.Status.COMPLETED);
        completedTriggerExecution.setWorkflowExecutionId(
            WorkflowExecutionId.of(PlatformType.AUTOMATION, 9L, "workflow-uuid", "trigger-b"));

        triggerExecutionService.update(completedTriggerExecution);

        TriggerExecution updatedTriggerExecution = triggerExecutionService.getTriggerExecution(
            triggerExecution.getId());

        assertThat(updatedTriggerExecution.getStatus()).isEqualTo(TriggerExecution.Status.COMPLETED);
        assertThat(updatedTriggerExecution.getStartDate()).isEqualTo(START_DATE);
        assertThat(updatedTriggerExecution.getWorkflowExecutionId()
            .getTriggerName()).isEqualTo("trigger-a");

        TriggerExecution restartedTriggerExecution = triggerExecutionService.getTriggerExecution(
            triggerExecution.getId());

        restartedTriggerExecution.setStartDate(START_DATE.plus(1, ChronoUnit.HOURS));
        restartedTriggerExecution.setStatus(TriggerExecution.Status.STARTED);

        triggerExecutionService.update(restartedTriggerExecution);

        updatedTriggerExecution = triggerExecutionService.getTriggerExecution(triggerExecution.getId());

        assertThat(updatedTriggerExecution.getStatus()).isEqualTo(TriggerExecution.Status.COMPLETED);
        assertThat(updatedTriggerExecution.getStartDate()).isEqualTo(START_DATE.plus(1, ChronoUnit.HOURS));
    }

    @Test
    void testGetTriggerExecutionsLoadsByIdAndSkipsTheRepositoryForNoIds() {
        TriggerExecution firstTriggerExecution = triggerExecutionService.create(
            getTriggerExecution("trigger-a", TriggerExecution.Status.COMPLETED, START_DATE));

        triggerExecutionService.create(getTriggerExecution("trigger-b", TriggerExecution.Status.COMPLETED, null));

        List<TriggerExecution> triggerExecutions = triggerExecutionService.getTriggerExecutions(
            List.of(firstTriggerExecution.getId()));

        assertThat(triggerExecutions).extracting(TriggerExecution::getId)
            .containsExactly(firstTriggerExecution.getId());
        assertThat(triggerExecutionService.getTriggerExecutions(List.of())).isEmpty();

        verify(triggerExecutionRepository, never()).findAllById(List.of());
    }

    private static TriggerExecution getTriggerExecution(
        String triggerName, TriggerExecution.Status status, Instant startDate) {

        return TriggerExecution.builder()
            .startDate(startDate)
            .status(status)
            .workflowExecutionId(WorkflowExecutionId.of(PlatformType.AUTOMATION, 9L, "workflow-uuid", triggerName))
            .workflowTrigger(new WorkflowTrigger(Map.of("name", "trigger", "type", "manual/v1/manual")))
            .build();
    }
}
