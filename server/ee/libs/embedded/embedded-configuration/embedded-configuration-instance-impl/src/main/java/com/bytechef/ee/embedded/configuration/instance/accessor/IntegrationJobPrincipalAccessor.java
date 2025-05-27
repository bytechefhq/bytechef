/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.instance.accessor;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessor;
import com.bytechef.platform.constant.ModeType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class IntegrationJobPrincipalAccessor implements JobPrincipalAccessor {

    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final IntegrationWorkflowService integrationWorkflowService;

    @SuppressFBWarnings("EI")
    public IntegrationJobPrincipalAccessor(
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationWorkflowService integrationWorkflowService) {

        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.integrationWorkflowService = integrationWorkflowService;
    }

    @Override
    public boolean isConnectionUsed(long connectionId) {
        return integrationInstanceConfigurationWorkflowService.isConnectionUsed(connectionId);
    }

    @Override
    public boolean isWorkflowEnabled(long jobPrincipalId, String workflowReferenceCode) {
        boolean workflowEnabled = false;

        if (integrationInstanceConfigurationService.isIntegrationInstanceConfigurationEnabled(jobPrincipalId) &&
            integrationInstanceConfigurationWorkflowService.isIntegrationInstanceWorkflowEnabled(
                jobPrincipalId, getWorkflowId(jobPrincipalId, workflowReferenceCode))) {

            workflowEnabled = true;
        }

        return workflowEnabled;
    }

    @Override
    public Map<String, ?> getInputMap(long jobPrincipalId, String workflowReferenceCode) {
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                jobPrincipalId, getWorkflowId(jobPrincipalId, workflowReferenceCode));

        return integrationInstanceConfigurationWorkflow.getInputs();
    }

    @Override
    public ModeType getType() {
        return ModeType.EMBEDDED;
    }

    @Override
    public String getWorkflowId(long jobPrincipalId, String workflowReferenceCode) {
        return integrationWorkflowService.getWorkflowId(jobPrincipalId, workflowReferenceCode);
    }

    @Override
    public String getLatestWorkflowId(String workflowReferenceCode) {
        return integrationWorkflowService.getLatestWorkflowId(workflowReferenceCode);
    }

    @Override
    public String getWorkflowReferenceCode(String workflowId) {
        IntegrationWorkflow workflowIntegrationWorkflow = integrationWorkflowService.getWorkflowIntegrationWorkflow(
            workflowId);

        return workflowIntegrationWorkflow.getWorkflowReferenceCode();
    }
}
