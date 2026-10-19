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

package com.bytechef.platform.job.sync.executor;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class SuspendingTasksTest {

    @Test
    void testApprovalAndWaitStepsAreSuspending() {
        assertThat(SuspendingTasks.isSuspending(workflowTask("approval/v1/requestApproval"))).isTrue();
        assertThat(SuspendingTasks.isSuspending(workflowTask("wait/v1/waitAfterTimeInterval"))).isTrue();
        assertThat(SuspendingTasks.isSuspending(workflowTask("waitForApproval/v1"))).isTrue();
        assertThat(SuspendingTasks.isSuspending(workflowTask("httpClient/v1/get"))).isFalse();
    }

    @Test
    void testAgentWithANestedApprovalGateIsSuspending() {
        WorkflowTask workflowTask = new WorkflowTask(
            Map.of(
                "name", "agent", "type", "aiAgent/v1/chat",
                "clusterElements", Map.of(
                    "tools", List.of(
                        Map.of(
                            "name", "gate", "type", "aiAgentUtils/v1/approvalGate",
                            "clusterElements", Map.of(
                                "tools", List.of(Map.of("name", "send", "type", "gmail/v1/sendEmail"))))))));

        assertThat(SuspendingTasks.isSuspending(workflowTask)).isTrue();
    }

    @Test
    void testAgentWithoutAnApprovalGateIsNotSuspending() {
        WorkflowTask workflowTask = new WorkflowTask(
            Map.of(
                "name", "agent", "type", "aiAgent/v1/chat",
                "clusterElements", Map.of(
                    "tools", List.of(Map.of("name", "search", "type", "googleSearch/v1/search")))));

        assertThat(SuspendingTasks.isSuspending(workflowTask)).isFalse();
    }

    private static WorkflowTask workflowTask(String type) {
        return new WorkflowTask(Map.of("name", "step", "type", type));
    }
}
