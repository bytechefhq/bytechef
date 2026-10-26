/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailCustomRule;
import com.bytechef.ee.platform.ai.guardrails.service.AiGuardrailCustomRuleService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
class AiGuardrailCustomRuleGraphQlController {

    private final AiGuardrailCustomRuleService aiGuardrailCustomRuleService;

    @SuppressFBWarnings("EI")
    AiGuardrailCustomRuleGraphQlController(AiGuardrailCustomRuleService aiGuardrailCustomRuleService) {
        this.aiGuardrailCustomRuleService = aiGuardrailCustomRuleService;
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasPermission(#workspaceId, 'Workspace', 'AI_GATEWAY_VIEW')")
    public List<AiGuardrailCustomRule> aiGuardrailCustomRules(@Argument long workspaceId) {
        return aiGuardrailCustomRuleService.getRules(workspaceId);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or "
        + "hasPermission(#input.workspaceId, 'Workspace', 'AI_GATEWAY_EDIT')")
    public AiGuardrailCustomRule createAiGuardrailCustomRule(@Argument AiGuardrailCustomRuleInput input) {
        Double contextScore = input.contextScore();

        return aiGuardrailCustomRuleService.create(
            new AiGuardrailCustomRule(
                input.workspaceId(), input.type(), input.pattern(), input.kind(),
                BigDecimal.valueOf(input.score()), input.contextKeywords(), input.contextWindow(),
                contextScore == null ? null : BigDecimal.valueOf(contextScore)));
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasPermission(#workspaceId, 'Workspace', 'AI_GATEWAY_EDIT')")
    public AiGuardrailCustomRule updateAiGuardrailCustomRulePattern(
        @Argument long workspaceId, @Argument long id, @Argument String pattern) {

        return aiGuardrailCustomRuleService.updatePattern(id, workspaceId, pattern);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasPermission(#workspaceId, 'Workspace', 'AI_GATEWAY_EDIT')")
    public AiGuardrailCustomRule setAiGuardrailCustomRuleEnabled(
        @Argument long workspaceId, @Argument long id, @Argument boolean enabled) {

        return aiGuardrailCustomRuleService.setEnabled(id, workspaceId, enabled);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasPermission(#workspaceId, 'Workspace', 'AI_GATEWAY_EDIT')")
    public boolean deleteAiGuardrailCustomRule(@Argument long workspaceId, @Argument long id) {
        aiGuardrailCustomRuleService.delete(id, workspaceId);

        return true;
    }

    record AiGuardrailCustomRuleInput(
        long workspaceId, String type, String pattern, int kind, double score,
        @Nullable String contextKeywords, @Nullable Integer contextWindow, @Nullable Double contextScore) {
    }
}
