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

package com.bytechef.automation.ai.agent.facade;

import com.bytechef.automation.ai.agent.domain.AiAgentChannel;
import com.bytechef.automation.ai.agent.domain.AiAgentElement;
import com.bytechef.automation.ai.agent.dto.AiAgentDTO;
import com.bytechef.automation.ai.agent.dto.AiAgentDeploymentDTO;
import com.bytechef.automation.ai.agent.dto.AiAgentVersionDTO;
import com.bytechef.automation.ai.agent.dto.ChatAgentDTO;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;
import java.util.Map;

/**
 * Facade for agent operations that span multiple services: provisioning the hidden backing project, keeping the
 * generated draft workflow in sync with every channel/element/instructions edit, and enforcing the cross-entity delete
 * rules (deployed or sub-agent-referenced agents cannot be deleted; the permanent {@code chat}/{@code workflowCall}
 * channels cannot be removed).
 *
 * @author Ivica Cardic
 */
public interface AiAgentFacade {

    AiAgentDTO createAgent(String title, String description, Long workspaceId);

    void deleteAgent(long id);

    AiAgentDTO getAgent(long id);

    List<AiAgentDTO> getAgents(Long workspaceId);

    /**
     * Read-model for the AiAgent Deployments page: every {@code ProjectDeployment} of every agent in
     * {@code workspaceId}'s hidden backing project, across every
     * {@link com.bytechef.platform.configuration.domain.Environment} —
     * {@code ProjectDeploymentService.getProjectDeployments(long)} (and every other projectId-scoped listing path)
     * excludes {@code __AI_AGENT__} projects, so this method assembles the list from the unfiltered per-environment
     * {@code fetchProjectDeployment(projectId, Environment)} lookup instead, the same mechanism
     * {@code AiAgentFacadeImpl.hasAnyDeployment} already relies on.
     */
    List<AiAgentDeploymentDTO> getAgentDeployments(Long workspaceId);

    /**
     * Read-model for the client's "Agent Chats" picker: every enabled, chat-reachable workflow of every agent in
     * {@code workspaceId} that has an enabled deployment in {@code environmentId}.
     *
     * <p>
     * Sibling of {@code workspaceChatWorkflows} (in {@code automation-configuration-graphql}), which structurally
     * cannot return these rows — agents live in hidden {@code __AI_AGENT__} projects that every projectId-scoped
     * {@code ProjectDeploymentService} listing path filters out. Rows carry a {@code workflowExecutionId} built exactly
     * the way that query builds it, so both feed the same client chat surface.
     * </p>
     */
    List<ChatAgentDTO> getWorkspaceChatAgents(Long workspaceId, long environmentId);

    /**
     * Partial update: a {@code null} {@code title}/{@code description}/{@code instructions} leaves that field's
     * existing value unchanged (same semantics as {@code updateAgentChannel}/{@code updateAgentElement}'s
     * {@code parameters}/{@code connectionId}) — there is no way to explicitly clear {@code description} or
     * {@code instructions} through this method today. {@code AgentInstructionsCard.tsx} is the only client caller and
     * sends only {@code instructions} on every save, so a "null clears" semantics here would silently wipe
     * {@code description} on every instructions edit.
     */
    AiAgentDTO updateAgent(long id, String title, String description, String instructions);

    /**
     * Replaces the agent's ENTIRE {@code settings} map (not a per-key merge) and regenerates the draft workflow —
     * built-in-tool emission reads {@code settings} on every generation, so a settings change is a draft-affecting
     * mutation like any other. Whole-map replace was chosen over merge for simplicity: the settings shape is small and
     * entirely client-owned (the Agent Settings UI always sends the complete object back), so there is no partial-field
     * editing use case a merge would serve. A {@code null} or empty {@code settings} resets every built-in tool to its
     * documented default (see {@code com.bytechef.automation.ai.agent.util.AiAgentSettings}) — there is no separate
     * "unset" state to preserve.
     */
    void updateAgentSettings(long id, Map<String, Object> settings);

    AiAgentChannel addAgentChannel(long agentId, String channelType, Map<String, Object> parameters, Long connectionId);

    /**
     * Partial update: a {@code null} {@code parameters} or {@code connectionId} leaves the existing value unchanged —
     * neither argument can be used to explicitly clear a previously-set value. There is currently no way to clear a
     * wired {@code connectionId} through this method; do a {@link #deleteAgentChannel(long)} followed by
     * {@link #addAgentChannel(long, String, Map, Long)} with {@code connectionId = null} instead.
     */
    void updateAgentChannel(long channelId, Map<String, Object> parameters, Long connectionId);

    void deleteAgentChannel(long channelId);

    AiAgentElement addAgentElement(
        long agentId, String kind, Long referenceId, Map<String, Object> parameters, Long connectionId);

    /**
     * Partial update: a {@code null} {@code parameters} or {@code connectionId} leaves the existing value unchanged —
     * see {@link #updateAgentChannel(long, Map, Long)}'s javadoc for the same caveat on clearing a
     * {@code connectionId}.
     */
    void updateAgentElement(long elementId, Map<String, Object> parameters, Long connectionId);

    void deleteAgentElement(long elementId);

    /**
     * Publishes the agent's backing project: regenerates the draft workflow from the agent's current
     * channels/elements/instructions, duplicates every workflow of the current draft version into a new published
     * {@code ProjectVersion}, and returns the version number of the fresh draft that follows.
     */
    int publishAgent(long id, String description);

    /**
     * Every tag attached to any agent in {@code workspaceId} — the option set the agents list's tag filter and the
     * per-row tag editor draw from.
     */
    /**
     * Every tag attached to an agent deployment in the workspace. Distinct from {@link #getAgentTags(Long)}: an agent
     * deployment is a {@code ProjectDeployment} and carries ordinary project-deployment tags, which are a different set
     * from the owning agent's own.
     */
    List<Tag> getAgentDeploymentTags(Long workspaceId);

    /**
     * The agent's version history, newest first — every {@code ProjectVersion} of its backing project, draft included.
     */
    List<AiAgentVersionDTO> getAgentVersions(long id);

    /**
     * The agent's configuration as a JSON document: title, description, instructions, settings, channels and elements.
     * <p>
     * Deliberately NOT a full backup. Connection ids are omitted — a connection belongs to a workspace and an
     * environment, so carrying one across would either dangle or point at someone else's credential. The generated
     * workflow, deployments and version history are omitted too: they are derived from this configuration, or from the
     * target instance's own state.
     */
    String exportAgent(long id);

    /**
     * Creates an agent in {@code workspaceId} from {@link #exportAgent(long)}'s JSON.
     * <p>
     * Elements that reference another row by id — {@code SKILL}, {@code SUB_AGENT}, {@code KNOWLEDGE_BASE} — are
     * skipped: those ids mean nothing in the target workspace, and importing them would generate a workflow pointing at
     * rows that do not exist. Channels arrive without their connections, so an imported agent needs re-wiring before it
     * can publish.
     */
    AiAgentDTO importAgent(long workspaceId, String json);

    List<Tag> getAgentTags(Long workspaceId);

    /**
     * Replaces the agent's ENTIRE tag set (not an add/remove delta) — tags not present in {@code tags} are detached.
     * Tags without an id are created first, so the client can send a freshly typed tag name. Unlike every other
     * mutation here this does NOT regenerate the draft workflow: tags are list-page metadata and the generator never
     * reads them.
     */
    /**
     * @param id the {@code ProjectDeployment} id — an agent deployment IS one, so its tags are project-deployment tags
     */
    void updateAgentDeploymentTags(long id, List<Tag> tags);

    void updateAgentTags(long id, List<Tag> tags);

    /**
     * Resolves the workflow id backing the agent's current draft (i.e. {@code project.getLastProjectVersion()}) — the
     * workflow the client's test chat panel executes against. Distinct from any workflow id belonging to a previously
     * published version.
     */
    String getDraftWorkflowId(long agentId);
}
