/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.IntegrationDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.component.domain.ConnectionDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.domain.BaseProperty;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class IntegrationInstanceConfigurationDTOAssembler {

    private final CategoryService categoryService;
    private final ComponentDefinitionService componentDefinitionService;
    private final ConnectionDefinitionService connectionDefinitionService;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final IntegrationService integrationService;
    private final IntegrationWorkflowService integrationWorkflowService;
    private final JobService jobService;
    private final PrincipalJobService principalJobService;
    private final TagService tagService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public IntegrationInstanceConfigurationDTOAssembler(
        CategoryService categoryService, ComponentDefinitionService componentDefinitionService,
        ConnectionDefinitionService connectionDefinitionService,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationService integrationService, IntegrationWorkflowService integrationWorkflowService,
        JobService jobService, PrincipalJobService principalJobService, TagService tagService,
        WorkflowService workflowService) {

        this.categoryService = categoryService;
        this.componentDefinitionService = componentDefinitionService;
        this.connectionDefinitionService = connectionDefinitionService;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.integrationService = integrationService;
        this.integrationWorkflowService = integrationWorkflowService;
        this.jobService = jobService;
        this.principalJobService = principalJobService;
        this.tagService = tagService;
        this.workflowService = workflowService;
    }

    public IntegrationInstanceConfigurationDTO toConnectedUserIntegrationInstanceConfigurationDTO(
        long integrationInstanceConfigurationId) {

        IntegrationInstanceConfiguration integrationInstanceConfiguration = integrationInstanceConfigurationService
            .getIntegrationInstanceConfiguration(integrationInstanceConfigurationId);

        List<IntegrationInstanceConfiguration> integrationInstanceConfigurations = List.of(
            integrationInstanceConfiguration);

        List<IntegrationDTO> integrationDTOs = getIntegrationDTOs(integrationInstanceConfigurations);

        return toConnectedUserIntegrationInstanceConfigurationDTO(
            integrationDTOs.getFirst(), integrationInstanceConfiguration, getTags(integrationInstanceConfigurations));
    }

    public IntegrationInstanceConfigurationDTO toConnectedUserIntegrationInstanceConfigurationDTO(
        IntegrationDTO integrationDTO, IntegrationInstanceConfiguration integrationInstanceConfiguration,
        List<Tag> tags) {

        return toIntegrationInstanceConfigurationDTO(
            integrationDTO, integrationInstanceConfiguration,
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(
                integrationInstanceConfiguration.getId()),
            integrationWorkflowService.getIntegrationWorkflows(
                integrationInstanceConfiguration.getIntegrationId(),
                integrationInstanceConfiguration.getIntegrationVersion()),
            tags);
    }

    public IntegrationInstanceConfigurationDTO toIntegrationInstanceConfigurationDTO(
        IntegrationInstanceConfiguration integrationInstanceConfiguration,
        List<IntegrationInstanceConfigurationWorkflow> integrationInstanceWorkflows,
        List<IntegrationWorkflow> integrationWorkflows) {

        return toIntegrationInstanceConfigurationDTO(
            toIntegrationDTO(integrationService.getIntegration(integrationInstanceConfiguration.getIntegrationId())),
            integrationInstanceConfiguration, integrationInstanceWorkflows, integrationWorkflows,
            tagService.getTags(integrationInstanceConfiguration.getTagIds()));
    }

    public IntegrationInstanceConfigurationDTO toIntegrationInstanceConfigurationDTO(
        IntegrationDTO integrationDTO, IntegrationInstanceConfiguration integrationInstanceConfiguration,
        List<IntegrationInstanceConfigurationWorkflow> integrationInstanceWorkflows,
        List<IntegrationWorkflow> integrationWorkflows, List<Tag> tags) {

        ConnectionDefinition connectionDefinition = connectionDefinitionService.getConnectionDefinition(
            integrationDTO.componentName(), null);

        List<String> authorizationPropertyNames = connectionDefinition.getAuthorizations()
            .stream()
            .flatMap(authorization -> CollectionUtils.stream(authorization.getProperties()))
            .map(BaseProperty::getName)
            .toList();

        List<String> connectionPropertyNames = connectionDefinition.getProperties()
            .stream()
            .map(BaseProperty::getName)
            .toList();

        return new IntegrationInstanceConfigurationDTO(
            filterParameters(integrationInstanceConfiguration.getConnectionParameters(), authorizationPropertyNames),
            filterParameters(integrationInstanceConfiguration.getConnectionParameters(), connectionPropertyNames),
            integrationInstanceConfiguration,
            CollectionUtils.map(
                integrationInstanceWorkflows,
                integrationInstanceConfigurationWorkflow -> {
                    String workflowUuid = getWorkflowUuid(
                        integrationInstanceConfigurationWorkflow.getWorkflowId(),
                        integrationInstanceConfiguration.getIntegrationVersion(), integrationWorkflows);

                    List<String> workflowUuidWorkflowIds = integrationWorkflows.stream()
                        .filter(integrationWorkflow -> Objects.equals(
                            integrationWorkflow.getUuidAsString(), workflowUuid))
                        .map(IntegrationWorkflow::getWorkflowId)
                        .toList();

                    return new IntegrationInstanceConfigurationWorkflowDTO(
                        integrationInstanceConfigurationWorkflow,
                        getWorkflowLastExecutionDate(
                            integrationInstanceConfiguration.getId(), workflowUuidWorkflowIds),
                        workflowService.getWorkflow(integrationInstanceConfigurationWorkflow.getWorkflowId()),
                        workflowUuid);
                }),
            integrationDTO,
            filterTags(tags, integrationInstanceConfiguration));
    }

    public List<IntegrationDTO> getIntegrationDTOs(
        List<IntegrationInstanceConfiguration> integrationInstanceConfigurations) {

        return integrationService.getIntegrations(
            integrationInstanceConfigurations.stream()
                .map(IntegrationInstanceConfiguration::getIntegrationId)
                .filter(Objects::nonNull)
                .toList())
            .stream()
            .map(this::toIntegrationDTO)
            .toList();
    }

    public List<Tag> getTags(List<IntegrationInstanceConfiguration> integrationInstanceConfigurations) {
        return tagService.getTags(
            integrationInstanceConfigurations
                .stream()
                .flatMap(integrationInstanceConfiguration -> CollectionUtils.stream(
                    integrationInstanceConfiguration.getTagIds()))
                .filter(Objects::nonNull)
                .toList());
    }

    private static Map<String, ?> filterParameters(Map<String, ?> parameters, List<String> propertyNames) {
        return parameters.entrySet()
            .stream()
            .filter(entry -> propertyNames.contains(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static List<Tag> filterTags(
        List<Tag> tags, IntegrationInstanceConfiguration integrationInstanceConfiguration) {

        List<Long> tagIds = integrationInstanceConfiguration.getTagIds();

        return CollectionUtils.filter(tags, tag -> tagIds.contains(tag.getId()));
    }

    private Category getCategory(Integration integration) {
        return integration.getCategoryId() == null ? null : categoryService.getCategory(integration.getCategoryId());
    }

    private Instant getJobEndDate(Long jobId) {
        Job job = jobService.getJob(jobId);

        return job.getEndDate();
    }

    private Instant getWorkflowLastExecutionDate(long integrationInstanceConfigurationId, List<String> workflowIds) {
        return principalJobService.fetchLastWorkflowJobId(
            integrationInstanceConfigurationId, workflowIds, PlatformType.EMBEDDED)
            .map(this::getJobEndDate)
            .orElse(null);
    }

    private static String getWorkflowUuid(
        String workflowId, Integer integrationVersion, List<IntegrationWorkflow> integrationWorkflows) {

        if (integrationVersion == null) {
            return null;
        }

        return integrationWorkflows.stream()
            .filter(integrationWorkflow -> Objects.equals(integrationWorkflow.getWorkflowId(), workflowId) &&
                integrationWorkflow.getIntegrationVersion() == integrationVersion)
            .findFirst()
            .map(IntegrationWorkflow::getUuidAsString)
            .orElseThrow();
    }

    private IntegrationDTO toIntegrationDTO(Integration integration) {
        return new IntegrationDTO(
            getCategory(integration),
            componentDefinitionService.getComponentDefinition(integration.getComponentName(), null),
            integration,
            integrationWorkflowService.getIntegrationWorkflowIds(
                integration.getId(), integration.getLastIntegrationVersion()),
            tagService.getTags(integration.getTagIds()));
    }
}
