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

package com.bytechef.platform.workflow.worker.task.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.exception.TaskExecutionException;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class AbstractTaskHandlerDryRunTest {

    @Mock
    private ActionDefinitionFacade actionDefinitionFacade;

    private AbstractTaskHandler handler;

    @BeforeEach
    void beforeEach() {
        handler = new AbstractTaskHandler("slack", 1, "sendMessage", actionDefinitionFacade) {};
    }

    @Test
    void testDryRunReturnsDeclaredOutputWithoutPerform() throws TaskExecutionException {
        when(actionDefinitionFacade.executeDryRunPerform("slack", 1, "sendMessage")).thenReturn(Map.of("ok", true));

        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(1L)
            .metadata(Map.of(MetadataConstants.DRY_RUN, true))
            .workflowTask(new WorkflowTask(Map.of("name", "sendMessage", "type", "slack/v1/sendMessage")))
            .build();

        assertThat(handler.handle(taskExecution)).isEqualTo(Map.of("ok", true));

        verify(actionDefinitionFacade, never()).executePerform(
            any(), anyInt(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            anyBoolean(), any(), any(), any());
    }
}
