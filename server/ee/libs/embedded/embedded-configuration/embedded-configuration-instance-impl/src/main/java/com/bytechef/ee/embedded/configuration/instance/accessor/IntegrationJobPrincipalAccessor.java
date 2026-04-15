/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.instance.accessor;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
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
public class IntegrationJobPrincipalAccessor implements JobPrincipalAccessor {

    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final IntegrationInstanceService integrationInstanceService;
    private final IntegrationWorkflowService integrationWorkflowService;

    @SuppressFBWarnings("EI")
    public IntegrationJobPrincipalAccessor(
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationInstanceService integrationInstanceService, IntegrationWorkflowService integrationWorkflowService) {

        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.integrationInstanceService = integrationInstanceService;
        this.integrationWorkflowService = integrationWorkflowService;
    }

    @Override
    public boolean isConnectionUsed(long connectionId) {
        return integrationInstanceConfigurationWorkflowService.isConnectionUsed(connectionId);
    }

    @Override
    public boolean isWorkflowEnabled(long jobPrincipalId, String workflowUuid) {
        boolean workflowEnabled = false;

        if (integrationInstanceConfigurationService.isIntegrationInstanceConfigurationEnabled(jobPrincipalId) &&
            integrationInstanceConfigurationWorkflowService.isIntegrationInstanceWorkflowEnabled(
                jobPrincipalId, getWorkflowId(jobPrincipalId, workflowUuid))) {

            workflowEnabled = true;
        }

        return workflowEnabled;
    }

    @Override
    public long getEnvironmentId(long jobPrincipalId) {
        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(jobPrincipalId);

        IntegrationInstanceConfiguration integrationInstanceConfiguration = integrationInstanceConfigurationService
            .getIntegrationInstanceConfiguration(integrationInstance.getIntegrationInstanceConfigurationId());

        Environment environment = integrationInstanceConfiguration.getEnvironment();

        return environment.ordinal();
    }

    @Override
    public Map<String, ?> getInputMap(long jobPrincipalId, String workflowUuid) {
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                jobPrincipalId, getWorkflowId(jobPrincipalId, workflowUuid));

        return integrationInstanceConfigurationWorkflow.getInputs();
    }

    @Override
    public Map<String, ?> getMetadataMap(long jobPrincipalId) {
        return Map.of();
    }

    @Override
    public PlatformType getType() {
        return PlatformType.EMBEDDED;
    }

    @Override
    public String getWorkflowId(long jobPrincipalId, String workflowUuid) {
        return integrationWorkflowService.getWorkflowId(jobPrincipalId, workflowUuid);
    }

    @Override
    public String getLastWorkflowId(String workflowUuid) {
        return integrationWorkflowService.getLastWorkflowId(workflowUuid);
    }

    @Override
    public String getWorkflowUuid(String workflowId) {
        IntegrationWorkflow integrationWorkflow = integrationWorkflowService.getWorkflowIntegrationWorkflow(
            workflowId);

        return integrationWorkflow.getUuidAsString();
    }

    @Override
    public void validateConnectionsForJob(long jobPrincipalId, String workflowUuid) {
        // Embedded integration instances do not model user-owned connections with reassignment lifecycle; the
        // automation-only ConnectionStatus.PENDING_REASSIGNMENT check does not apply here.
    }
}
