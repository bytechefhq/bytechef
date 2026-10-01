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

package com.bytechef.automation.ai.agent.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI_EXPOSE_REP")
public record AiAgentDeploymentDTO(
    long id, String name, long agentId, String agentTitle, long projectId, int environmentId, boolean enabled,
    int projectVersion, List<AiAgentDeploymentWorkflowDTO> workflows,
    @Nullable Instant lastExecutionDate) {

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public record AiAgentDeploymentWorkflowDTO(
        String workflowId, boolean enabled, List<AiAgentDeploymentTriggerDTO> triggers) {
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public record AiAgentDeploymentTriggerDTO(
        String name, String type, Map<String, ?> parameters, @Nullable String staticWebhookUrl) {
    }
}
