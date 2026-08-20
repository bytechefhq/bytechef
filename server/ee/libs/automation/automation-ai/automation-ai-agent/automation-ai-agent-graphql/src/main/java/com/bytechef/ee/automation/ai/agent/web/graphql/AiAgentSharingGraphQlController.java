/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.agent.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.agent.facade.AiAgentSharingFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.domain.ResourceVisibility;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for agent visibility and sharing.
 *
 * <p>
 * Authorization is enforced on {@link AiAgentSharingFacade} and, beyond it, on the {@code ProjectSharingFacade} it
 * delegates to — not here, so it protects every caller of either facade rather than this entry point alone. This class
 * only maps arguments, exactly as {@code ProjectSharingGraphQlController} does.
 *
 * <p>
 * The {@code long} arguments are primitives on purpose: {@code #agentId} is only a usable gate key while the parameter
 * cannot be null, since a boxed {@code null} reaches {@code AutomationPermissionEvaluator} as a null target id. The
 * schema declares {@code ID!}, but that is a second file's promise.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
public class AiAgentSharingGraphQlController {

    private final AiAgentSharingFacade aiAgentSharingFacade;

    @SuppressFBWarnings("EI")
    public AiAgentSharingGraphQlController(AiAgentSharingFacade aiAgentSharingFacade) {
        this.aiAgentSharingFacade = aiAgentSharingFacade;
    }

    @QueryMapping
    public List<Long> aiAgentGrants(@Argument long agentId) {
        return aiAgentSharingFacade.getAgentGrants(agentId);
    }

    @MutationMapping
    public boolean setAiAgentVisibility(@Argument long agentId, @Argument ResourceVisibility visibility) {
        aiAgentSharingFacade.setAgentVisibility(agentId, visibility);

        return true;
    }

    @MutationMapping
    public boolean grantAiAgentAccess(@Argument long agentId, @Argument long userId) {
        aiAgentSharingFacade.grantAgentAccess(agentId, userId);

        return true;
    }

    @MutationMapping
    public boolean revokeAiAgentAccess(@Argument long agentId, @Argument long userId) {
        aiAgentSharingFacade.revokeAgentAccess(agentId, userId);

        return true;
    }
}
