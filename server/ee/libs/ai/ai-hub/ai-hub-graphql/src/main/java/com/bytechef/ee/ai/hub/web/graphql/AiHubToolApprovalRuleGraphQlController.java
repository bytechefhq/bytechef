/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.web.graphql;

import com.bytechef.ee.ai.hub.approval.AiHubToolApproval;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalRule;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalRuleFacade;
import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL surface for a workspace's {@link AiHubToolApprovalRule} rows. Read access requires the workspace VIEWER role;
 * mutations require the tenant ADMIN authority because a rule change alters which tool calls are gated for every member
 * of the workspace.
 *
 * <p>
 * Authorization is enforced on {@link AiHubToolApprovalRuleFacade}, not here.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnAiHub
public class AiHubToolApprovalRuleGraphQlController {

    private final AiHubToolApprovalRuleFacade toolApprovalRuleFacade;

    @SuppressFBWarnings("EI")
    public AiHubToolApprovalRuleGraphQlController(AiHubToolApprovalRuleFacade toolApprovalRuleFacade) {
        this.toolApprovalRuleFacade = toolApprovalRuleFacade;
    }

    @QueryMapping
    public List<AiHubToolApprovalRule> aiHubToolApprovalRules(@Argument long workspaceId) {
        return toolApprovalRuleFacade.getRules(workspaceId);
    }

    @QueryMapping
    public List<String> aiHubToolApprovalDefaultToolNames() {
        return toolApprovalRuleFacade.getDefaultToolNames();
    }

    @MutationMapping
    public AiHubToolApprovalRule createAiHubToolApprovalRule(
        @Argument long workspaceId, @Argument AiHubToolApproval.ToolKind toolKind, @Argument String toolName,
        @Argument AiHubToolApprovalRule.Mode mode) {

        return toolApprovalRuleFacade.createRule(workspaceId, toolKind, toolName, mode);
    }

    @MutationMapping
    public boolean deleteAiHubToolApprovalRule(@Argument long workspaceId, @Argument long ruleId) {
        toolApprovalRuleFacade.deleteRule(workspaceId, ruleId);

        return true;
    }

    @SchemaMapping(typeName = "AiHubToolApprovalRule", field = "toolKind")
    public String toolApprovalRuleToolKind(AiHubToolApprovalRule rule) {
        return rule.getToolKind()
            .name();
    }

    @SchemaMapping(typeName = "AiHubToolApprovalRule", field = "mode")
    public String toolApprovalRuleMode(AiHubToolApprovalRule rule) {
        return rule.getMode()
            .name();
    }
}
