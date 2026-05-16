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

package com.bytechef.platform.worker.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.component.definition.ActionDefinition.WebSocketHandler;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class RealtimeActionTaskExecutionPostOutputProcessorTest {

    private final RealtimeActionTaskExecutionPostOutputProcessor processor =
        new RealtimeActionTaskExecutionPostOutputProcessor();

    @Test
    void testRealtimeActionInAWorkflowFailsWithAnExplanation() {
        WebSocketHandler webSocketHandler = webSocketEmitter -> {};

        // The alternative was worse than a failure: the old processor blocked on an emitter nothing could ever
        // complete, so the task held a worker thread until the JVM restarted.
        assertThatThrownBy(() -> processor.process(createTaskExecution(), webSocketHandler))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("realtime action")
            .hasMessageContaining("websocketTasks");
    }

    @Test
    void testOrdinaryOutputPassesThrough() {
        Object output = "some output";

        assertThat(processor.process(createTaskExecution(), output)).isSameAs(output);
    }

    private static TaskExecution createTaskExecution() {
        return TaskExecution.builder()
            .id(1L)
            .jobId(2L)
            .build();
    }
}
