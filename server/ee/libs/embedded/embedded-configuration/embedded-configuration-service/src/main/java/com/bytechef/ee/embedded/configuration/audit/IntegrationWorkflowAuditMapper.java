/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.audit;

import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class IntegrationWorkflowAuditMapper implements AuditMapper {

    private static final String WORKFLOW_ID_KEY = "workflowId";

    private final IntegrationWorkflowService integrationWorkflowService;

    @SuppressFBWarnings("EI")
    public IntegrationWorkflowAuditMapper(IntegrationWorkflowService integrationWorkflowService) {
        this.integrationWorkflowService = integrationWorkflowService;
    }

    @Override
    public Map<String, String> map(AuditInvocation auditInvocation) {
        if (!(auditInvocation.result() instanceof Long integrationWorkflowId)) {
            return Map.of();
        }

        IntegrationWorkflow integrationWorkflow = integrationWorkflowService.getIntegrationWorkflow(
            integrationWorkflowId);

        return Map.of(WORKFLOW_ID_KEY, integrationWorkflow.getWorkflowId());
    }
}
