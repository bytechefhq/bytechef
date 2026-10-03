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

package com.bytechef.platform.ai.a2a;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class A2AAgentRunTest {

    private static final A2ATaskReference TASK_REFERENCE = new A2ATaskReference("task-1", "context-1");

    @Test
    void testStartedKeepsTheTaskReference() {
        CompletableFuture<A2AAgentResult> result = new CompletableFuture<>();

        A2AAgentRun agentRun = A2AAgentRun.started(TASK_REFERENCE, result);

        assertThat(agentRun.taskReference()).isEqualTo(TASK_REFERENCE);
        assertThat(agentRun.result()).isSameAs(result);
    }

    @Test
    void testStartedRequiresATaskReference() {
        assertThatNullPointerException()
            .isThrownBy(() -> A2AAgentRun.started(null, new CompletableFuture<>()))
            .withMessage("taskReference");
    }

    @Test
    void testOfTakesTheTaskReferenceOfTheResult() {
        A2AAgentRun agentRun = A2AAgentRun.of(A2AAgentResult.ofWorking("running", TASK_REFERENCE));

        assertThat(agentRun.taskReference()).isEqualTo(TASK_REFERENCE);
        assertThat(agentRun.result()).isCompleted();
    }
}
