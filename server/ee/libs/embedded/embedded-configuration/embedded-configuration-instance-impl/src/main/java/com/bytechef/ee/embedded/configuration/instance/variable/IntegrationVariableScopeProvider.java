/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.instance.variable;

import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.platform.variable.domain.VariableScope;
import com.bytechef.ee.platform.variable.provider.VariableScopeProvider;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Embedded is tenant-scoped: every integration-instance job and every integration workflow reads the single embedded
 * (organization) variable set. The by-workflow lookup still checks ownership so an automation workflow id never falls
 * into the embedded scope.
 *
 * @version ee
 */
@Component
@ConditionalOnEEVersion
public class IntegrationVariableScopeProvider implements VariableScopeProvider {

    private final IntegrationWorkflowService integrationWorkflowService;

    @SuppressFBWarnings("EI")
    public IntegrationVariableScopeProvider(IntegrationWorkflowService integrationWorkflowService) {
        this.integrationWorkflowService = integrationWorkflowService;
    }

    @Override
    public PlatformType getType() {
        return PlatformType.EMBEDDED;
    }

    @Override
    public Optional<VariableScope> getVariableScope(long jobPrincipalId) {
        return Optional.of(VariableScope.embedded());
    }

    @Override
    public Optional<VariableScope> getVariableScopeByWorkflowId(String workflowId) {
        return integrationWorkflowService.fetchWorkflowIntegrationWorkflow(workflowId)
            .map(integrationWorkflow -> VariableScope.embedded());
    }
}
