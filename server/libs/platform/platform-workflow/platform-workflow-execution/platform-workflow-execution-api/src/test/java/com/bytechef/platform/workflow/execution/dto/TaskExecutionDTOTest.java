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

package com.bytechef.platform.workflow.execution.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Anshul Goel
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class TaskExecutionDTOTest {

    @Test
    void buildHierarchyShouldGroupOnErrorDispatcherTasksByBranch() {
        WorkflowTask onErrorWorkflowTask = new WorkflowTask(
            Map.of(
                "name", "onError",
                "type", "on-error/v1",
                "parameters", Map.of(
                    "main-branch", List.of(
                        Map.of("name", "mainTask", "type", "test/v1")),
                    "on-error-branch", List.of(
                        Map.of("name", "errorTask", "type", "test/v1")))));

        TaskExecution onErrorTaskExecution = TaskExecution.builder()
            .id(1L)
            .workflowTask(onErrorWorkflowTask)
            .build();

        TaskExecution mainTaskExecution = TaskExecution.builder()
            .id(2L)
            .parentId(1L)
            .workflowTask(new WorkflowTask(Map.of("name", "mainTask", "type", "test/v1")))
            .build();

        TaskExecution errorTaskExecution = TaskExecution.builder()
            .id(3L)
            .parentId(1L)
            .workflowTask(new WorkflowTask(Map.of("name", "errorTask", "type", "test/v1")))
            .build();

        List<TaskExecutionDTO> flatTaskExecutions = List.of(
            new TaskExecutionDTO(onErrorTaskExecution, "On Error", "icon", Map.of(), null, null),
            new TaskExecutionDTO(mainTaskExecution, "Main Task", "icon", Map.of(), null, null),
            new TaskExecutionDTO(errorTaskExecution, "Error Task", "icon", Map.of(), null, null));

        List<TaskExecutionDTO> hierarchy = TaskExecutionDTO.buildHierarchy(flatTaskExecutions);

        assertEquals(1, hierarchy.size());

        TaskExecutionDTO onErrorTaskExecutionDTO = hierarchy.getFirst();

        assertTrue(onErrorTaskExecutionDTO.children()
            .isEmpty());
        assertEquals(2, onErrorTaskExecutionDTO.iterations()
            .size());
        assertEquals(1, onErrorTaskExecutionDTO.iterations()
            .get(0)
            .size());
        assertEquals("mainTask", onErrorTaskExecutionDTO.iterations()
            .get(0)
            .getFirst()
            .workflowTask()
            .getName());
        assertEquals(1, onErrorTaskExecutionDTO.iterations()
            .get(1)
            .size());
        assertEquals("errorTask", onErrorTaskExecutionDTO.iterations()
            .get(1)
            .getFirst()
            .workflowTask()
            .getName());
    }

    @Test
    void buildHierarchyShouldPreserveBranchOrderWhenOnlyErrorBranchHasTasks() {
        WorkflowTask onErrorWorkflowTask = new WorkflowTask(
            Map.of(
                "name", "onError",
                "type", "on-error/v1",
                "parameters", Map.of(
                    "main-branch", List.of(
                        Map.of("name", "mainTask", "type", "test/v1")),
                    "on-error-branch", List.of(
                        Map.of("name", "errorTask", "type", "test/v1")))));

        TaskExecution onErrorTaskExecution = TaskExecution.builder()
            .id(1L)
            .workflowTask(onErrorWorkflowTask)
            .build();

        TaskExecution errorTaskExecution = TaskExecution.builder()
            .id(2L)
            .parentId(1L)
            .workflowTask(new WorkflowTask(Map.of("name", "errorTask", "type", "test/v1")))
            .build();

        List<TaskExecutionDTO> flatTaskExecutions = List.of(
            new TaskExecutionDTO(onErrorTaskExecution, "On Error", "icon", Map.of(), null, null),
            new TaskExecutionDTO(errorTaskExecution, "Error Task", "icon", Map.of(), null, null));

        List<TaskExecutionDTO> hierarchy = TaskExecutionDTO.buildHierarchy(flatTaskExecutions);

        TaskExecutionDTO onErrorTaskExecutionDTO = hierarchy.getFirst();

        assertEquals(2, onErrorTaskExecutionDTO.iterations()
            .size());
        assertTrue(onErrorTaskExecutionDTO.iterations()
            .get(0)
            .isEmpty());
        assertEquals(1, onErrorTaskExecutionDTO.iterations()
            .get(1)
            .size());
        assertEquals("errorTask", onErrorTaskExecutionDTO.iterations()
            .get(1)
            .getFirst()
            .workflowTask()
            .getName());
    }
}
