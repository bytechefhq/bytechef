/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.IntegrationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.cache.WorkflowCacheManager;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeOutputFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class IntegrationWorkflowFacadeImpl implements IntegrationWorkflowFacade {

    private final EnvironmentService environmentService;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final IntegrationService integrationService;
    private final IntegrationWorkflowService integrationWorkflowService;
    private final WorkflowCacheManager workflowCacheManager;
    private final WorkflowService workflowService;
    private final WorkflowFacade workflowFacade;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    public IntegrationWorkflowFacadeImpl(
        EnvironmentService environmentService,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationService integrationService, IntegrationWorkflowService integrationWorkflowService,
        WorkflowCacheManager workflowCacheManager, WorkflowService workflowService, WorkflowFacade workflowFacade,
        WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.environmentService = environmentService;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.integrationService = integrationService;
        this.integrationWorkflowService = integrationWorkflowService;
        this.workflowCacheManager = workflowCacheManager;
        this.workflowService = workflowService;
        this.workflowFacade = workflowFacade;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @Override
    public long addWorkflow(long integrationId, String definition) {
        Integration integration = integrationService.getIntegration(integrationId);

        Workflow workflow = workflowService.create(definition, Workflow.Format.JSON, Workflow.SourceType.JDBC);

        IntegrationWorkflow integrationWorkflow = integrationWorkflowService.addWorkflow(
            integrationId, integration.getLastIntegrationVersion(), workflow.getId());

        return integrationWorkflow.getId();
    }

    @Override
    public void deleteWorkflow(String workflowId) {
        Integration integration = integrationService.getWorkflowIntegration(workflowId);

        List<IntegrationInstanceConfiguration> integrationInstanceConfigurations =
            integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(integration.getId());

        for (IntegrationInstanceConfiguration integrationInstanceConfiguration : integrationInstanceConfigurations) {
            List<IntegrationInstanceConfigurationWorkflow> integrationInstanceConfigurationWorkflows =
                integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(
                    Validate.notNull(integrationInstanceConfiguration.getId(), "id"));

            if (CollectionUtils.anyMatch(
                integrationInstanceConfigurationWorkflows,
                integrationInstanceConfigurationWorkflow -> Objects.equals(
                    integrationInstanceConfigurationWorkflow.getWorkflowId(), workflowId))) {

                integrationInstanceConfigurationWorkflows.stream()
                    .filter(
                        integrationInstanceConfigurationWorkflow -> Objects.equals(
                            integrationInstanceConfigurationWorkflow.getWorkflowId(), workflowId))
                    .findFirst()
                    .ifPresent(
                        integrationInstanceConfigurationWorkflow -> integrationInstanceConfigurationWorkflowService
                            .delete(integrationInstanceConfigurationWorkflow.getId()));
            }
        }

        integrationWorkflowService.delete(
            integration.getId(), integration.getLastIntegrationVersion(), workflowId);

        workflowTestConfigurationService.delete(workflowId);

        workflowService.delete(workflowId);
    }

    @Override
    @Transactional(readOnly = true)
    public IntegrationWorkflowDTO getIntegrationWorkflow(String workflowId) {
        IntegrationWorkflow integrationWorkflow = integrationWorkflowService.getWorkflowIntegrationWorkflow(workflowId);

        return new IntegrationWorkflowDTO(workflowFacade.getWorkflow(workflowId), integrationWorkflow);
    }

    @Override
    @Transactional(readOnly = true)
    public IntegrationWorkflowDTO getIntegrationWorkflow(long integrationWorkflowId) {
        IntegrationWorkflow integrationWorkflow = integrationWorkflowService.getIntegrationWorkflow(
            integrationWorkflowId);

        return new IntegrationWorkflowDTO(
            workflowFacade.getWorkflow(integrationWorkflow.getWorkflowId()), integrationWorkflow);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntegrationWorkflowDTO> getIntegrationWorkflows() {
        return integrationWorkflowService.getIntegrationWorkflows()
            .stream()
            .map(integrationWorkflows -> workflowFacade.fetchWorkflow(integrationWorkflows.getWorkflowId())
                .map(workflowDTO -> new IntegrationWorkflowDTO(workflowDTO, integrationWorkflows))
                .orElse(null))
            .filter(Objects::nonNull)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntegrationWorkflowDTO> getIntegrationWorkflows(long integrationId) {
        Integration integration = integrationService.getIntegration(integrationId);

        return integrationWorkflowService
            .getIntegrationWorkflows(integration.getId(), integration.getLastIntegrationVersion())
            .stream()
            .map(integrationWorkflow -> new IntegrationWorkflowDTO(
                workflowFacade.getWorkflow(integrationWorkflow.getWorkflowId()), integrationWorkflow))
            .sorted(
                (integrationWorkflow1, integrationWorkflow2) -> {
                    String label1 = integrationWorkflow1.getLabel();
                    String label2 = integrationWorkflow2.getLabel();

                    return label1.compareToIgnoreCase(label2);
                })
            .toList();
    }

    @Override
    public List<IntegrationWorkflowDTO> getIntegrationVersionWorkflows(
        long id, int integrationVersion, boolean includeAllFields) {

        List<IntegrationWorkflow> integrationWorkflows = integrationWorkflowService.getIntegrationWorkflows(
            id, integrationVersion);

        if (includeAllFields) {
            return CollectionUtils.map(
                integrationWorkflows,
                integrationWorkflow -> new IntegrationWorkflowDTO(
                    workflowFacade.getWorkflow(integrationWorkflow.getWorkflowId()), integrationWorkflow));
        } else {
            return CollectionUtils.map(
                integrationWorkflows,
                integrationWorkflow -> new IntegrationWorkflowDTO(
                    workflowService.getWorkflow(integrationWorkflow.getWorkflowId()), integrationWorkflow));
        }
    }

    @Override
    public void updateWorkflow(String workflowId, String definition, int version) {
        workflowService.update(workflowId, definition, version);

        for (String cacheName : WorkflowNodeOutputFacade.WORKFLOW_CACHE_NAMES) {
            for (Environment environment : environmentService.getEnvironments()) {
                workflowCacheManager.clearCacheForWorkflow(workflowId, cacheName, environment.ordinal());
            }
        }
    }
}
