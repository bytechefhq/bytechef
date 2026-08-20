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

import com.bytechef.platform.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Read-model for the AiAgent Deployments page: a single {@code ProjectDeployment} of an agent's hidden backing project,
 * flattened with the owning agent's display title and its workflow's per-trigger channel info.
 *
 * <p>
 * {@code ProjectDeploymentService.getProjectDeployments(long)} (and every other projectId-scoped listing path) filters
 * out {@code __AI_AGENT__} projects (see {@code SystemProjects}), so this DTO is assembled by
 * {@code AiAgentFacade#getAgentDeployments(long)} from the unfiltered {@code fetchProjectDeployment(projectId,
 * Environment)} lookup instead — the same mechanism {@code AiAgentFacadeImpl.hasAnyDeployment} already relies on.
 * </p>
 *
 * @param id                the {@code ProjectDeployment} id
 * @param name              the {@code ProjectDeployment}'s own name (distinct from {@code agentTitle}) — the client
 *                          needs this to round-trip an edit through the shared {@code ProjectDeploymentDialog}, whose
 *                          update call sends the full record back and requires a non-null name
 * @param agentId           the owning agent's id
 * @param agentTitle        the owning agent's display title
 * @param projectId         the agent's hidden backing project id — the client needs this to drive
 *                          {@code ProjectDeploymentDialog}'s change-project-version step
 * @param environmentId     the deployment's {@code Environment} ordinal
 * @param enabled           whether the deployment is enabled
 * @param projectVersion    the deployed project version
 * @param workflows         per-workflow trigger info for this deployment
 * @param tags              the deployment's OWN tags. An agent deployment is a {@code ProjectDeployment}, so these are
 *                          ordinary project-deployment tags and the client edits them through that same mutation — they
 *                          are not the owning agent's tags, which live on {@code ai_agent_tag}
 * @param lastExecutionDate when this deployment's workflow last finished running, or {@code null} if it never has —
 *                          derived the same way {@code ProjectDeploymentFacadeImpl} derives its own, from the last job
 *                          recorded against the deployment
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI_EXPOSE_REP")
public record AiAgentDeploymentDTO(
    long id, String name, long agentId, String agentTitle, long projectId, int environmentId, boolean enabled,
    int projectVersion, List<AiAgentDeploymentWorkflowDTO> workflows, List<Tag> tags,
    @Nullable Instant lastExecutionDate) {

    /**
     * @param workflowId the deployment workflow's workflow id
     * @param enabled    whether this workflow is enabled within the deployment
     * @param triggers   the workflow's triggers, in definition order — each carries its OWN
     *                   {@link AiAgentDeploymentTriggerDTO#staticWebhookUrl}, since different triggers on the same
     *                   workflow (e.g. a slack channel and a telegram channel) resolve to different webhook URLs
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public record AiAgentDeploymentWorkflowDTO(
        String workflowId, boolean enabled, List<AiAgentDeploymentTriggerDTO> triggers) {
    }

    /**
     * @param name             the trigger's workflow node name
     * @param type             the trigger's workflow node type string (e.g. {@code chat/v1/chat},
     *                         {@code schedule/v1/cron})
     * @param parameters       the trigger's raw (unevaluated) parameters
     * @param staticWebhookUrl this trigger's resolved static webhook URL when its {@code TriggerDefinition.getType()}
     *                         is {@code STATIC_WEBHOOK} (other than {@code manual}), or {@code null} otherwise —
     *                         notably {@code null} for a {@code DYNAMIC_WEBHOOK} trigger (e.g. Telegram, which
     *                         self-registers its webhook with the provider rather than exposing a URL to copy)
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public record AiAgentDeploymentTriggerDTO(
        String name, String type, Map<String, ?> parameters, @Nullable String staticWebhookUrl) {
    }
}
