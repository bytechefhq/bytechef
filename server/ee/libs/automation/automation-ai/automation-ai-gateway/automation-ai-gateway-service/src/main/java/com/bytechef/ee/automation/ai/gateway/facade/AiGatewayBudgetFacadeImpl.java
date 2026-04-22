/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.ee.automation.ai.gateway.service.WorkspaceAiGatewayBudgetService;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayBudget;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayBudgetService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link AiGatewayBudgetFacade}. Delegates to the shared services and carries the authorization
 * guards so they are enforced for every caller of the facade.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
class AiGatewayBudgetFacadeImpl implements AiGatewayBudgetFacade {

    private final AiGatewayBudgetService aiGatewayBudgetService;
    private final WorkspaceAiGatewayBudgetService workspaceAiGatewayBudgetService;

    @SuppressFBWarnings("EI")
    AiGatewayBudgetFacadeImpl(
        AiGatewayBudgetService aiGatewayBudgetService,
        WorkspaceAiGatewayBudgetService workspaceAiGatewayBudgetService) {

        this.aiGatewayBudgetService = aiGatewayBudgetService;
        this.workspaceAiGatewayBudgetService = workspaceAiGatewayBudgetService;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")
    public Optional<AiGatewayBudget> getBudgetByWorkspaceId(long workspaceId) {
        return workspaceAiGatewayBudgetService.getBudgetByWorkspaceId(workspaceId);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiGatewayBudget getBudget(long id) {
        return aiGatewayBudgetService.getBudget(id);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiGatewayBudget createInWorkspace(AiGatewayBudget budget, long workspaceId) {
        return workspaceAiGatewayBudgetService.createInWorkspace(budget, workspaceId);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void delete(long id) {
        aiGatewayBudgetService.delete(id);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiGatewayBudget update(AiGatewayBudget budget) {
        return aiGatewayBudgetService.update(budget);
    }
}
