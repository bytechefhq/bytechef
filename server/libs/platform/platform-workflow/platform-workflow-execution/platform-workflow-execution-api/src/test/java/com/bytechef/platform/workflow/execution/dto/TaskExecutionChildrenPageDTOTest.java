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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class TaskExecutionChildrenPageDTOTest {

    @Test
    void rejectsBothChildrenAndIterations() {
        List<TaskExecutionDTO> children = Collections.nCopies(1, null);

        assertThatThrownBy(
            () -> new TaskExecutionChildrenPageDTO(children, Map.of(), List.of(List.of()), 0, 50, 1, 1L, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void collectsEachIconOnceByTypeAndClearsTheRowIcons() {
        TaskExecutionDTO firstQuery = taskExecutionDTO(1L, "postgresql/v1/query", "<svg>postgresql</svg>");
        TaskExecutionDTO secondQuery = taskExecutionDTO(2L, "postgresql/v1/query", "<svg>postgresql</svg>");
        TaskExecutionDTO condition = taskExecutionDTO(3L, "condition/v1", "<svg>condition</svg>");
        TaskExecutionDTO withoutIcon = taskExecutionDTO(4L, "var/v1/set", null);

        TaskExecutionChildrenPageDTO page = new TaskExecutionChildrenPageDTO(
            List.of(), List.of(List.of(firstQuery, condition), List.of(secondQuery, withoutIcon)), 0, 50, 2, 2L, 1);

        assertThat(page.icons()).containsExactlyInAnyOrderEntriesOf(
            Map.of("postgresql/v1/query", "<svg>postgresql</svg>", "condition/v1", "<svg>condition</svg>"));
        assertThat(page.iterations()).hasSize(2);
        assertThat(page.iterations()
            .stream()
            .flatMap(List::stream))
                .extracting(TaskExecutionDTO::id, TaskExecutionDTO::icon)
                .containsExactly(tuple(1L, null), tuple(3L, null), tuple(2L, null), tuple(4L, null));
    }

    @Test
    void collectsIconsFromChildren() {
        TaskExecutionDTO child = taskExecutionDTO(1L, "condition/v1", "<svg>condition</svg>");

        TaskExecutionChildrenPageDTO page = new TaskExecutionChildrenPageDTO(
            List.of(child), List.of(), 0, 50, 1, 1L, 1);

        assertThat(page.icons()).containsExactlyEntriesOf(Map.of("condition/v1", "<svg>condition</svg>"));
        assertThat(page.children()).singleElement()
            .satisfies(taskExecutionDTO -> {
                assertThat(taskExecutionDTO.id()).isEqualTo(1L);
                assertThat(taskExecutionDTO.icon()).isNull();
                assertThat(taskExecutionDTO.title()).isEqualTo("Task 1");
            });
    }

    private static TaskExecutionDTO taskExecutionDTO(long id, String type, String icon) {
        TaskExecution taskExecution = TaskExecution.builder()
            .id(id)
            .workflowTask(new WorkflowTask(Map.of("name", "task" + id, "type", type)))
            .build();

        return new TaskExecutionDTO(taskExecution, "Task " + id, icon, Map.of(), null, null);
    }
}
