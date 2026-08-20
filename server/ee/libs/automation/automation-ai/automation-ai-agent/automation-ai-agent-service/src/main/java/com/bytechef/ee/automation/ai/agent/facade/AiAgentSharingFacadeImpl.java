/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.agent.facade;

import com.bytechef.automation.ai.agent.domain.AiAgent;
import com.bytechef.automation.ai.agent.exception.AiAgentErrorType;
import com.bytechef.automation.ai.agent.service.AiAgentService;
import com.bytechef.ee.automation.configuration.facade.ProjectSharingFacade;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.domain.ResourceVisibility;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves the agent to its hidden backing project and hands the operation to {@link ProjectSharingFacade}. Every
 * method here is two lines for a reason: an agent's reach IS its project's, so a second implementation of the sharing
 * rules could only drift from the first.
 *
 * <p>
 * <b>The gate, and why it is not the project one.</b> {@code ProjectSharingFacadeImpl} requires
 * {@code isResourceOwner('Project', …) || hasResourceRole(…, 'Project', 'ADMIN')}. Copying that expression across with
 * {@code 'AiAgent'} substituted would not be the same rule — it would be a stricter one that nobody could satisfy:
 * {@code AiAgentOwnershipResolver} answers with a workspace and no owner user, so {@code isResourceOwner('AiAgent', …)}
 * is unconditionally false and the whole expression collapses to "workspace ADMIN only", locking an agent's own creator
 * out of sharing it. The gate is therefore the one the rest of the agent facade uses, in the vocabulary the rest of the
 * agent surface answers to: {@code AGENT_EDIT} on the agent, EDITOR rank, the same scope every other change to an agent
 * takes.
 *
 * <p>
 * <b>The second disjunct is what keeps a withheld agent from being stranded.</b> {@code AGENT_EDIT} on
 * {@code 'AiAgent'} carries the visibility precondition {@code AiAgentVisibilityProvider} registers, and the resolver's
 * admin bypass is TENANT admin — so a WORKSPACE admin who is neither the agent's creator nor a grantee cannot see a
 * withheld agent and would be denied by the precondition before the inner gate was ever reached. That would leave an
 * agent whose creator set it PRIVATE re-shareable by nobody but its creator and a tenant admin, while the same person
 * could re-share a PRIVATE <em>project</em> freely: {@code ProjectSharingFacadeImpl}'s gate consults ownership and
 * workspace role, never visibility. {@code @permissionService.hasResourceRole(#agentId, 'AiAgent', 'ADMIN')} is the
 * identical predicate the project facade already trusts, keyed on the agent so it resolves through
 * {@code AiAgentOwnershipResolver} to the same workspace — it restores exactly the reach a workspace admin has over
 * projects and nothing beyond it. It deliberately does not make workspace admins able to SEE withheld agents anywhere
 * else: every other {@code 'AiAgent'}-keyed gate still runs the precondition alone.
 *
 * <p>
 * <b>What actually authorizes a share is the two gates together</b>, and neither is redundant. The outer one adds the
 * agent scope and the visibility precondition, which the inner one has no equivalent of. The inner one keeps the
 * project model's restriction of audience changes to the resource's own creator or a workspace admin, which
 * {@code AGENT_EDIT} alone would not impose. The effective rule is (({@code AGENT_EDIT} on the agent AND visible) OR
 * workspace ADMIN) AND (creator of its backing project OR workspace ADMIN); tenant admins short circuit both. For
 * anyone who is not a workspace admin that is unchanged: {@code AGENT_EDIT}, visible, and the backing project's
 * creator.
 *
 * <p>
 * {@code getAgentGrants} takes {@code AGENT_EDIT} rather than {@code AGENT_VIEW} deliberately. The audience of a
 * withheld agent is not part of seeing it — the project sibling makes the same call in its own javadoc — and using the
 * read scope here would put a laxer outer gate on the one method that discloses who else was let in.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class AiAgentSharingFacadeImpl implements AiAgentSharingFacade {

    private final AiAgentService aiAgentService;
    private final ProjectSharingFacade projectSharingFacade;

    @SuppressFBWarnings("EI")
    public AiAgentSharingFacadeImpl(AiAgentService aiAgentService, ProjectSharingFacade projectSharingFacade) {
        this.aiAgentService = aiAgentService;
        this.projectSharingFacade = projectSharingFacade;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#agentId, 'AiAgent', 'AGENT_EDIT') || " +
        "@permissionService.hasResourceRole(#agentId, 'AiAgent', 'ADMIN')")
    public List<Long> getAgentGrants(long agentId) {
        AiAgent agent = getAgent(agentId);

        return projectSharingFacade.getProjectGrants(getWorkspaceId(agent), agent.getProjectId());
    }

    @Override
    @PreAuthorize("hasPermission(#agentId, 'AiAgent', 'AGENT_EDIT') || " +
        "@permissionService.hasResourceRole(#agentId, 'AiAgent', 'ADMIN')")
    public void grantAgentAccess(long agentId, long userId) {
        AiAgent agent = getAgent(agentId);

        projectSharingFacade.grantProjectAccess(getWorkspaceId(agent), agent.getProjectId(), userId);
    }

    @Override
    @PreAuthorize("hasPermission(#agentId, 'AiAgent', 'AGENT_EDIT') || " +
        "@permissionService.hasResourceRole(#agentId, 'AiAgent', 'ADMIN')")
    public void revokeAgentAccess(long agentId, long userId) {
        AiAgent agent = getAgent(agentId);

        projectSharingFacade.revokeProjectAccess(getWorkspaceId(agent), agent.getProjectId(), userId);
    }

    @Override
    @PreAuthorize("hasPermission(#agentId, 'AiAgent', 'AGENT_EDIT') || " +
        "@permissionService.hasResourceRole(#agentId, 'AiAgent', 'ADMIN')")
    public void setAgentVisibility(long agentId, ResourceVisibility visibility) {
        AiAgent agent = getAgent(agentId);

        projectSharingFacade.setProjectVisibility(getWorkspaceId(agent), agent.getProjectId(), visibility);
    }

    /**
     * {@code fetchAgent}, not {@code getAgent}: the latter throws {@code NoSuchElementException} for an unknown id,
     * which would leave this facade as an unhandled 500 rather than the typed error a caller can act on. Same
     * reasoning, and the same shape, as {@code ProjectSharingFacadeImpl.validateProjectBelongsToWorkspace}.
     */
    private AiAgent getAgent(long agentId) {
        return aiAgentService.fetchAgent(agentId)
            .orElseThrow(() -> invalidAgent(agentId));
    }

    /**
     * {@code ai_agent.workspace_id} is nullable, so this is a real row shape rather than a theoretical one. It is
     * unreachable through an ordinary caller — {@code AiAgentOwnershipResolver} resolves a workspace-less agent to
     * {@code ResourceOwner.unknown()} and the gate above denies — but a tenant admin and a skip-checks context both
     * short circuit that gate, and the project facade's {@code workspaceId} argument has to come from somewhere.
     * Failing here rather than substituting a default keeps the two paths agreeing that such an agent belongs to
     * nobody.
     */
    private static long getWorkspaceId(AiAgent agent) {
        Long workspaceId = agent.getWorkspaceId();

        if (workspaceId == null) {
            throw invalidAgent(agent.getId());
        }

        return workspaceId;
    }

    /**
     * An unknown agent and a workspace-less one raise the identical error, from one factory rather than two copies that
     * could drift — and deliberately the same one, so a caller cannot tell the two apart by probing ids.
     */
    private static ConfigurationException invalidAgent(Long agentId) {
        return new ConfigurationException(
            "Agent id=%s cannot be shared".formatted(agentId), AiAgentErrorType.INVALID_AGENT);
    }
}
