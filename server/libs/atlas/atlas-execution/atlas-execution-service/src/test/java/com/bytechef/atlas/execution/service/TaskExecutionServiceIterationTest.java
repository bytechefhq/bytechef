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

package com.bytechef.atlas.execution.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.repository.memory.InMemoryTaskExecutionRepository;
import com.bytechef.tenant.TenantContext;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class TaskExecutionServiceIterationTest {

    private static final long PARENT_ID = 10L;

    private TaskExecutionService taskExecutionService;
    private InMemoryTaskExecutionRepository taskExecutionRepository;

    @BeforeEach
    void beforeEach() {
        TenantContext.resetCurrentTenantId();

        taskExecutionRepository = new InMemoryTaskExecutionRepository();
        taskExecutionService = new TaskExecutionServiceImpl(taskExecutionRepository);

        TenantContext.setCurrentTenantId("T");
    }

    @AfterEach
    void afterEach() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void iterationIndexesAndCountAgreeWhenTheLoopStartsWithAnIterationStart() {
        List<TaskExecution> childTaskExecutions = saveChildren(0, 1, 0, 1);

        assertThat(taskExecutionService.getIterationCounts(List.of(PARENT_ID))).isEqualTo(Map.of(PARENT_ID, 2L));
        assertThat(getIterationIndexes(childTaskExecutions)).containsExactly(0, 0, 1, 1);
    }

    @Test
    void iterationIndexesAndCountAgreeWhenTheLoopStartsWithAPartialIteration() {
        List<TaskExecution> childTaskExecutions = saveChildren(1, 0, 1, 0, 1);

        assertThat(taskExecutionService.getIterationCounts(List.of(PARENT_ID))).isEqualTo(Map.of(PARENT_ID, 3L));
        assertThat(getIterationIndexes(childTaskExecutions)).containsExactly(0, 1, 1, 2, 2);
    }

    @Test
    void iterationCountIsOneWhenNoChildStartsAnIteration() {
        List<TaskExecution> childTaskExecutions = saveChildren(-1, -1);

        assertThat(taskExecutionService.getIterationCounts(List.of(PARENT_ID))).isEqualTo(Map.of(PARENT_ID, 1L));
        assertThat(getIterationIndexes(childTaskExecutions)).containsExactly(0, 0);
    }

    @Test
    void childIndexIsThePositionInCreationOrder() {
        List<TaskExecution> childTaskExecutions = saveChildren(-1, -1, -1);

        assertThat(
            childTaskExecutions.stream()
                .map(taskExecutionService::getChildIndex)
                .toList())
                    .containsExactly(0, 1, 2);
    }

    private List<Integer> getIterationIndexes(List<TaskExecution> taskExecutions) {
        return taskExecutions.stream()
            .map(taskExecutionService::getIterationIndex)
            .toList();
    }

    private List<TaskExecution> saveChildren(int... taskNumbers) {
        Instant baseDate = Instant.parse("2026-01-01T00:00:00Z");
        List<TaskExecution> taskExecutions = new ArrayList<>();

        for (int index = 0; index < taskNumbers.length; index++) {
            TaskExecution taskExecution = new TaskExecution();

            taskExecution.setJobId(1L);
            taskExecution.setParentId(PARENT_ID);
            taskExecution.setTaskNumber(taskNumbers[index]);

            setCreatedDate(taskExecution, baseDate.plusSeconds(index));

            taskExecutions.add(taskExecutionRepository.save(taskExecution));
        }

        return taskExecutions;
    }

    private static void setCreatedDate(TaskExecution taskExecution, Instant createdDate) {
        try {
            Field createdDateField = TaskExecution.class.getDeclaredField("createdDate");

            createdDateField.setAccessible(true);
            createdDateField.set(taskExecution, createdDate);
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new IllegalStateException(reflectiveOperationException);
        }
    }
}
