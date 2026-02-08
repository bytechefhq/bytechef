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

package com.bytechef.platform.workflow.coordinator.message;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.coordinator.event.TaskExecutionCompleteEvent;
import com.bytechef.atlas.coordinator.event.TaskExecutionErrorEvent;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.message.event.MessageEvent;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class OutputFilteringMessageEventPostReceiveProcessorTest {

    private OutputFilteringMessageEventPostReceiveProcessor processor;

    @BeforeEach
    void beforeEach() {
        processor = new OutputFilteringMessageEventPostReceiveProcessor();
    }

    @Test
    void testProcessPassesThroughTaskExecutionCompleteEvent() {
        TaskExecution taskExecution = TaskExecution.builder()
            .id(1L)
            .jobId(100L)
            .workflowTask(
                new WorkflowTask(
                    Map.of(WorkflowConstants.NAME, "accelo_1", WorkflowConstants.TYPE, "accelo/v1/createContact",
                        WorkflowConstants.PARAMETERS, Map.of())))
            .build();

        taskExecution.putMetadata(MetadataConstants.OUTPUT_REFERENCE_PATHS, Set.of("response.lastname"));

        TaskExecutionCompleteEvent event = new TaskExecutionCompleteEvent(taskExecution);

        MessageEvent<?> result = processor.process(event);

        assertThat(result).isSameAs(event);
    }

    @Test
    void testProcessPassesThroughNonCompleteEvent() {
        TaskExecution taskExecution = TaskExecution.builder()
            .id(1L)
            .jobId(100L)
            .workflowTask(
                new WorkflowTask(
                    Map.of(WorkflowConstants.NAME, "accelo_1", WorkflowConstants.TYPE, "accelo/v1/createContact",
                        WorkflowConstants.PARAMETERS, Map.of())))
            .build();

        TaskExecutionErrorEvent event = new TaskExecutionErrorEvent(taskExecution);

        MessageEvent<?> result = processor.process(event);

        assertThat(result).isSameAs(event);
    }

    @Test
    void testProcessPassesThroughWithoutReferencePaths() {
        TaskExecution taskExecution = TaskExecution.builder()
            .id(1L)
            .jobId(100L)
            .workflowTask(
                new WorkflowTask(
                    Map.of(WorkflowConstants.NAME, "accelo_1", WorkflowConstants.TYPE, "accelo/v1/createContact",
                        WorkflowConstants.PARAMETERS, Map.of())))
            .build();

        TaskExecutionCompleteEvent event = new TaskExecutionCompleteEvent(taskExecution);

        MessageEvent<?> result = processor.process(event);

        assertThat(result).isSameAs(event);
    }
}
