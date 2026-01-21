/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflowConnection;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.IntegrationDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.exception.IntegrationInstanceConfigurationErrorType;
import com.bytechef.ee.embedded.configuration.exception.IntegrationWorkflowErrorType;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.component.domain.ConnectionDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.exception.ConnectionErrorType;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.domain.BaseProperty;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class IntegrationInstanceConfigurationFacadeImpl implements IntegrationInstanceConfigurationFacade {

    private final CategoryService categoryService;
    private final ComponentDefinitionService componentDefinitionService;
    private final ConnectedUserService connectedUserService;
    private final ConnectionService connectionService;
    private final ConnectionDefinitionService connectionDefinitionService;
    private final EnvironmentService environmentService;
    private final PrincipalJobFacade principalJobFacade;
    private final PrincipalJobService principalJobService;
    private final JobFacade jobFacade;
    private final JobService jobService;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final IntegrationInstanceFacade integrationInstanceFacade;
    private final IntegrationInstanceService integrationInstanceService;
    private final IntegrationInstanceWorkflowService integrationInstanceWorkflowService;
    private final IntegrationService integrationService;
    private final IntegrationWorkflowService integrationWorkflowService;
    private final OAuth2Service oAuth2Service;
    private final TagService tagService;
    private final TriggerExecutionService triggerExecutionService;
    private final ComponentConnectionFacade componentConnectionFacade;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public IntegrationInstanceConfigurationFacadeImpl(
        CategoryService categoryService, ComponentDefinitionService componentDefinitionService,
        ConnectedUserService connectedUserService, ConnectionService connectionService,
        ConnectionDefinitionService connectionDefinitionService, EnvironmentService environmentService,
        PrincipalJobFacade principalJobFacade,
        PrincipalJobService principalJobService, JobFacade jobFacade, JobService jobService,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationInstanceFacade integrationInstanceFacade, IntegrationInstanceService integrationInstanceService,
        IntegrationInstanceWorkflowService integrationInstanceWorkflowService, IntegrationService integrationService,
        IntegrationWorkflowService integrationWorkflowService, OAuth2Service oAuth2Service, TagService tagService,
        TriggerExecutionService triggerExecutionService, ComponentConnectionFacade componentConnectionFacade,
        WorkflowService workflowService) {

        this.categoryService = categoryService;
        this.componentDefinitionService = componentDefinitionService;
        this.connectedUserService = connectedUserService;
        this.connectionService = connectionService;
        this.connectionDefinitionService = connectionDefinitionService;
        this.environmentService = environmentService;
        this.principalJobFacade = principalJobFacade;
        this.principalJobService = principalJobService;
        this.jobFacade = jobFacade;
        this.jobService = jobService;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.integrationInstanceFacade = integrationInstanceFacade;
        this.integrationInstanceService = integrationInstanceService;
        this.integrationInstanceWorkflowService = integrationInstanceWorkflowService;
        this.integrationService = integrationService;
        this.integrationWorkflowService = integrationWorkflowService;
        this.oAuth2Service = oAuth2Service;
        this.tagService = tagService;
        this.triggerExecutionService = triggerExecutionService;
        this.componentConnectionFacade = componentConnectionFacade;
        this.workflowService = workflowService;
    }

    @Override
    public long createIntegrationInstanceConfiguration(
        IntegrationInstanceConfigurationDTO integrationInstanceConfigurationDTO) {

        IntegrationInstanceConfiguration integrationInstanceConfiguration = integrationInstanceConfigurationDTO
            .toIntegrationInstanceConfiguration();

        long integrationId = Validate.notNull(integrationInstanceConfiguration.getIntegrationId(), "integrationId");

        Integration integration = integrationService.getIntegration(integrationId);

        if (!integration.isPublished()) {
            throw new ConfigurationException(
                "Integration id=%s is not published".formatted(integrationId),
                IntegrationInstanceConfigurationErrorType.INTEGRATION_NOT_PUBLISHED);
        }

        if (integration.getLastIntegrationVersion() == integrationInstanceConfiguration.getIntegrationVersion()) {
            throw new ConfigurationException(
                "Integration version v=%s cannot be in DRAFT".formatted(
                    integrationInstanceConfiguration.getIntegrationVersion()),
                IntegrationInstanceConfigurationErrorType.INVALID_INTEGRATION_VERSION);
        }

        int integrationVersion = integrationInstanceConfiguration.getIntegrationVersion();

        List<IntegrationInstanceConfiguration> integrationInstanceConfigurations =
            integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(
                integrationInstanceConfiguration.getEnvironment(), integrationId, null);

        if (integrationInstanceConfigurations.stream()
            .anyMatch(curIntegrationInstanceConfigurations -> curIntegrationInstanceConfigurations
                .getVersion() == integrationVersion)) {

            throw new ConfigurationException(
                "Instance Configuration already exists for environment=%s and integrationVersion=%s".formatted(
                    integrationInstanceConfiguration.getEnvironment(), integrationVersion),
                IntegrationInstanceConfigurationErrorType.INSTANCE_CONFIGURATION_EXISTS);
        }

        List<Tag> tags = checkTags(integrationInstanceConfigurationDTO.tags());

        if (!tags.isEmpty()) {
            integrationInstanceConfiguration.setTags(tags);
        }

        if (MapUtils.isEmpty(integrationInstanceConfiguration.getConnectionParameters())) {
            integrationInstanceConfiguration.setConnectionParameters(
                oAuth2Service.checkPredefinedParameters(
                    integration.getComponentName(), integrationInstanceConfiguration.getConnectionParameters()));
        }

        integrationInstanceConfiguration = integrationInstanceConfigurationService.create(
            integrationInstanceConfiguration);

        checkIntegrationInstanceConfigurationWorkflows(
            integrationInstanceConfiguration, -1,
            CollectionUtils.map(
                integrationInstanceConfigurationDTO.integrationInstanceConfigurationWorkflows(),
                IntegrationInstanceConfigurationWorkflowDTO::toIntegrationInstanceConfigurationWorkflow),
            List.of());

        return integrationInstanceConfiguration.getId();
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public long createIntegrationInstanceConfigurationWorkflowJob(Long id, String workflowId) {
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(id, workflowId);

        return principalJobFacade.createJob(
            new JobParametersDTO(workflowId, integrationInstanceConfigurationWorkflow.getInputs()), id,
            PlatformType.EMBEDDED);
    }

    @Override
    public void deleteIntegrationInstanceConfiguration(long id) {
        IntegrationInstanceConfiguration integrationInstanceConfiguration = integrationInstanceConfigurationService
            .getIntegrationInstanceConfiguration(id);

        if (integrationInstanceConfiguration.isEnabled()) {
            enableIntegrationInstanceConfiguration(integrationInstanceConfiguration.getId(), false);
        }

        List<IntegrationInstanceConfigurationWorkflow> integrationInstanceConfigurationWorkflows =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(id);

        List<Long> jobIds = principalJobService.getJobIds(id, PlatformType.EMBEDDED);

        for (long jobId : jobIds) {
            triggerExecutionService.deleteJobTriggerExecution(jobId);

            principalJobService.deletePrincipalJobs(jobId, PlatformType.EMBEDDED);

            jobFacade.deleteJob(jobId);
        }

        for (IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow : integrationInstanceConfigurationWorkflows) {

            integrationInstanceConfigurationWorkflowService.delete(integrationInstanceConfigurationWorkflow.getId());
        }

        integrationInstanceConfigurationService.delete(id);

// TODO find a way to delete ll tags not referenced anymore
//        integration.getTagIds()
//            .forEach(tagService::delete);
    }

    @Override
    public void enableIntegrationInstanceConfiguration(long integrationInstanceConfigurationId, boolean enable) {
        List<IntegrationInstance> integrationInstances = integrationInstanceService.getIntegrationInstances(
            integrationInstanceConfigurationId);

        for (IntegrationInstance integrationInstance : integrationInstances) {
            if (integrationInstance.isEnabled()) {
                ConnectedUser connectedUser = connectedUserService.getConnectedUser(
                    integrationInstance.getConnectedUserId());

                if (!connectedUser.isEnabled()) {
                    continue;
                }

                integrationInstanceFacade.enableIntegrationInstanceWorkflowTriggers(
                    integrationInstance.getId(), enable);
            }
        }

        integrationInstanceConfigurationService.updateEnabled(integrationInstanceConfigurationId, enable);
    }

    @Override
    public void enableIntegrationInstanceConfigurationWorkflow(
        long integrationInstanceConfigurationId, String workflowId, boolean enable) {

        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            doEnableIntegrationInstanceConfigurationWorkflow(integrationInstanceConfigurationId, workflowId, enable);

        integrationInstanceConfigurationWorkflowService.updateEnabled(
            integrationInstanceConfigurationWorkflow.getId(), enable);
    }

    @Override
    @Transactional(readOnly = true)
    public IntegrationInstanceConfigurationDTO getIntegrationInstanceConfigurationIntegration(
        long integrationId, boolean enabled, Environment environment) {

        return getIntegrationInstanceConfigurationIntegrations(true, environment)
            .stream()
            .filter(integrationInstanceConfigurationDTO -> {
                IntegrationDTO integrationDTO = integrationInstanceConfigurationDTO.integration();

                return Objects.equals(integrationDTO.id(), integrationId);
            })
            .findFirst()
            .orElseThrow(() -> new ConfigurationException(
                "Integration instance configuration not found",
                IntegrationInstanceConfigurationErrorType.INSTANCE_CONFIGURATION_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntegrationInstanceConfigurationDTO> getIntegrationInstanceConfigurationIntegrations(
        boolean enabled, Environment environment) {

        List<IntegrationInstanceConfigurationDTO> integrationInstanceConfigurationDTOs = List.of();

        List<IntegrationInstanceConfiguration> integrationInstanceConfigurations =
            integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(environment, true);

        if (!integrationInstanceConfigurations.isEmpty()) {
            List<IntegrationDTO> integrationDTOs = getIntegrations(integrationInstanceConfigurations);
            List<Tag> tags = getTags(integrationInstanceConfigurations);

            integrationInstanceConfigurationDTOs = integrationInstanceConfigurations.stream()
                .map(integrationInstanceConfiguration -> {
                    IntegrationDTO integrationDTO = integrationDTOs.stream()
                        .filter(curIntegration -> Objects.equals(
                            curIntegration.id(), integrationInstanceConfiguration.getIntegrationId()))
                        .findFirst()
                        .orElseThrow();

                    return toIntegrationInstanceConfigurationDTO(
                        integrationDTO,
                        integrationInstanceConfiguration,
                        integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(
                            integrationInstanceConfiguration.getId()),
                        integrationWorkflowService.getIntegrationWorkflows(
                            integrationInstanceConfiguration.getIntegrationId(),
                            integrationInstanceConfiguration.getIntegrationVersion()),
                        filterTags(tags, integrationInstanceConfiguration));
                })
                .toList();
        }

        return integrationInstanceConfigurationDTOs;
    }

    @Override
    @Transactional(readOnly = true)
    public IntegrationInstanceConfigurationDTO getIntegrationInstanceConfiguration(long id) {
        IntegrationInstanceConfiguration integrationInstanceConfiguration = integrationInstanceConfigurationService
            .getIntegrationInstanceConfiguration(id);

        List<IntegrationWorkflow> integrationWorkflows = integrationWorkflowService.getIntegrationWorkflows(
            integrationInstanceConfiguration.getIntegrationId(),
            integrationInstanceConfiguration.getIntegrationVersion());
        List<String> workflowIds = getWorkflowIds(integrationInstanceConfiguration);

        return toIntegrationInstanceConfigurationDTO(
            integrationInstanceConfiguration,
            CollectionUtils.filter(
                integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(
                    integrationInstanceConfiguration.getId()),
                integrationInstanceConfigurationWorkflow -> workflowIds
                    .contains(integrationInstanceConfigurationWorkflow.getWorkflowId())),
            integrationWorkflows);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tag> getIntegrationInstanceConfigurationTags() {
        List<IntegrationInstanceConfiguration> integrationInstanceConfigurations =
            integrationInstanceConfigurationService.getIntegrationInstanceConfigurations();

        return tagService.getTags(
            integrationInstanceConfigurations
                .stream()
                .map(IntegrationInstanceConfiguration::getTagIds)
                .flatMap(Collection::stream)
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntegrationInstanceConfigurationDTO> getIntegrationInstanceConfigurations(
        Long environmentId, Long integrationId, Long tagId, boolean includeAllFields) {

        Environment environment = environmentId == null ? null : environmentService.getEnvironment(environmentId);

        List<IntegrationInstanceConfiguration> integrationInstanceConfigurations =
            integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(
                environment, integrationId, tagId);

        if (includeAllFields) {
            List<IntegrationInstanceConfigurationWorkflow> integrationInstanceConfigurationWorkflows =
                integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(
                    CollectionUtils.map(integrationInstanceConfigurations, IntegrationInstanceConfiguration::getId));
            List<IntegrationDTO> integrationDTOs = getIntegrations(integrationInstanceConfigurations);
            List<Tag> tags = getTags(integrationInstanceConfigurations);

            return CollectionUtils.map(
                integrationInstanceConfigurations,
                integrationInstanceConfiguration -> {
                    List<String> workflowIds = getWorkflowIds(integrationInstanceConfiguration);

                    return toIntegrationInstanceConfigurationDTO(
                        CollectionUtils.getFirst(
                            integrationDTOs, integration -> Objects.equals(
                                integration.id(), integrationInstanceConfiguration.getIntegrationId())),
                        integrationInstanceConfiguration,
                        CollectionUtils.filter(
                            integrationInstanceConfigurationWorkflows,
                            integrationInstanceConfigurationWorkflow -> Objects.equals(
                                integrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationId(),
                                integrationInstanceConfiguration.getId()) &&
                                workflowIds.contains(integrationInstanceConfigurationWorkflow.getWorkflowId())),
                        integrationWorkflowService.getIntegrationWorkflows(
                            integrationInstanceConfiguration.getIntegrationId(),
                            integrationInstanceConfiguration.getIntegrationVersion()),
                        filterTags(tags, integrationInstanceConfiguration));
                });
        } else {
            return CollectionUtils.map(integrationInstanceConfigurations, IntegrationInstanceConfigurationDTO::new);
        }
    }

    @Override
    public void updateIntegrationInstanceConfiguration(
        IntegrationInstanceConfigurationDTO integrationInstanceConfigurationDTO) {

        IntegrationInstanceConfiguration integrationInstanceConfiguration = integrationInstanceConfigurationDTO
            .toIntegrationInstanceConfiguration();

        List<Tag> tags = checkTags(integrationInstanceConfigurationDTO.tags());

        if (!tags.isEmpty()) {
            integrationInstanceConfiguration.setTags(tags);
        }

        IntegrationInstanceConfiguration oldIntegrationInstanceConfiguration = integrationInstanceConfigurationService
            .getIntegrationInstanceConfiguration(integrationInstanceConfigurationDTO.id());

        integrationInstanceConfiguration = integrationInstanceConfigurationService.update(
            integrationInstanceConfiguration);

        checkIntegrationInstanceConfigurationWorkflows(
            integrationInstanceConfiguration, oldIntegrationInstanceConfiguration.getIntegrationVersion(),
            CollectionUtils.map(
                integrationInstanceConfigurationDTO.integrationInstanceConfigurationWorkflows(),
                IntegrationInstanceConfigurationWorkflowDTO::toIntegrationInstanceConfigurationWorkflow),
            integrationWorkflowService.getIntegrationWorkflows(integrationInstanceConfiguration.getIntegrationId()));
    }

    @Override
    public void updateIntegrationInstanceConfigurationTags(long id, List<Tag> tags) {
        integrationInstanceConfigurationService.update(id, CollectionUtils.map(checkTags(tags), Tag::getId));
    }

    @Override
    public void updateIntegrationInstanceConfigurationWorkflow(
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow) {

        validateIntegrationInstanceConfigurationWorkflow(integrationInstanceConfigurationWorkflow);

        integrationInstanceConfigurationWorkflowService.update(integrationInstanceConfigurationWorkflow);
    }

    private void checkIntegrationInstanceConfigurationWorkflows(
        IntegrationInstanceConfiguration integrationInstanceConfiguration, int oldIntegrationVersion,
        List<IntegrationInstanceConfigurationWorkflow> integrationInstanceConfigurationWorkflows,
        List<IntegrationWorkflow> allIntegrationWorkflows) {

        List<IntegrationInstanceConfigurationWorkflow> oldIntegrationInstanceConfigurationWorkflows = List.of();

        if (oldIntegrationVersion != -1) {
            oldIntegrationInstanceConfigurationWorkflows =
                integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(
                    integrationInstanceConfiguration.getId());
        }

        for (IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow : integrationInstanceConfigurationWorkflows) {

            IntegrationInstanceConfigurationWorkflow oldIntegrationInstanceConfigurationWorkflow = null;

            if (oldIntegrationVersion != -1) {
                String workflowUuid = allIntegrationWorkflows.stream()
                    .filter(curIntegrationWorkflow -> Objects.equals(
                        curIntegrationWorkflow.getWorkflowId(),
                        integrationInstanceConfigurationWorkflow.getWorkflowId()))
                    .findFirst()
                    .map(IntegrationWorkflow::getUuidAsString)
                    .orElseThrow(() -> new ConfigurationException(
                        "Integration workflow with workflowId=%s not found".formatted(
                            integrationInstanceConfigurationWorkflow.getWorkflowId()),
                        IntegrationWorkflowErrorType.INTEGRATION_WORKFLOW_NOT_FOUND));

                String oldWorkflowId = allIntegrationWorkflows.stream()
                    .filter(curIntegrationWorkflow -> Objects.equals(
                        curIntegrationWorkflow.getUuidAsString(), workflowUuid) &&
                        curIntegrationWorkflow.getIntegrationVersion() == oldIntegrationVersion)
                    .map(IntegrationWorkflow::getWorkflowId)
                    .findFirst()
                    .orElse(null);

                if (oldWorkflowId != null) {
                    oldIntegrationInstanceConfigurationWorkflow = oldIntegrationInstanceConfigurationWorkflows.stream()
                        .filter(curIntegrationInstanceConfigurationWorkflow -> Objects.equals(
                            curIntegrationInstanceConfigurationWorkflow.getWorkflowId(),
                            oldWorkflowId))
                        .findFirst()
                        .orElse(null);
                }
            }

            validateIntegrationInstanceConfigurationWorkflow(integrationInstanceConfigurationWorkflow);

            if (oldIntegrationInstanceConfigurationWorkflow == null) {
                integrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationId(
                    integrationInstanceConfiguration.getId());

                integrationInstanceConfigurationWorkflowService.create(integrationInstanceConfigurationWorkflow);

                if (integrationInstanceConfiguration.isEnabled() &&
                    integrationInstanceConfigurationWorkflow.isEnabled()) {

                    enableIntegrationInstanceConfigurationWorkflow(
                        integrationInstanceConfiguration.getId(),
                        integrationInstanceConfigurationWorkflow.getWorkflowId(), true);
                }
            } else {
                boolean oldIntegrationInstanceConfigurationWorkflowEnabled =
                    oldIntegrationInstanceConfigurationWorkflow.isEnabled();

                String oldWorkflowId = oldIntegrationInstanceConfigurationWorkflow.getWorkflowId();

                oldIntegrationInstanceConfigurationWorkflow
                    .setConnections(integrationInstanceConfigurationWorkflow.getConnections());
                oldIntegrationInstanceConfigurationWorkflow
                    .setEnabled(integrationInstanceConfigurationWorkflow.isEnabled());
                oldIntegrationInstanceConfigurationWorkflow
                    .setInputs(integrationInstanceConfigurationWorkflow.getInputs());
                oldIntegrationInstanceConfigurationWorkflow
                    .setWorkflowId(integrationInstanceConfigurationWorkflow.getWorkflowId());

                if (integrationInstanceConfigurationWorkflow.isEnabled()) {
                    integrationInstanceConfigurationWorkflowService.update(oldIntegrationInstanceConfigurationWorkflow);

                    if (integrationInstanceConfiguration.isEnabled()) {
                        if (oldIntegrationInstanceConfigurationWorkflowEnabled) {
                            doEnableIntegrationInstanceConfigurationWorkflow(
                                integrationInstanceConfiguration.getId(),
                                integrationInstanceConfigurationWorkflow.getWorkflowId(), false);
                            doEnableIntegrationInstanceConfigurationWorkflow(
                                integrationInstanceConfiguration.getId(),
                                integrationInstanceConfigurationWorkflow.getWorkflowId(), true);
                        } else {
                            doEnableIntegrationInstanceConfigurationWorkflow(
                                integrationInstanceConfiguration.getId(),
                                integrationInstanceConfigurationWorkflow.getWorkflowId(), true);
                        }
                    }
                } else {
                    if (oldIntegrationInstanceConfigurationWorkflowEnabled) {
                        doEnableIntegrationInstanceConfigurationWorkflow(
                            integrationInstanceConfiguration.getId(), oldWorkflowId, false);
                    }

                    integrationInstanceConfigurationWorkflowService.update(oldIntegrationInstanceConfigurationWorkflow);
                }
            }
        }

        for (IntegrationInstanceConfigurationWorkflow oldIntegrationInstanceConfigurationWorkflow : oldIntegrationInstanceConfigurationWorkflows) {

            String workflowUuid = allIntegrationWorkflows.stream()
                .filter(curIntegrationWorkflow -> Objects.equals(
                    curIntegrationWorkflow.getWorkflowId(),
                    oldIntegrationInstanceConfigurationWorkflow.getWorkflowId()))
                .findFirst()
                .map(IntegrationWorkflow::getUuidAsString)
                .orElseThrow(() -> new ConfigurationException(
                    "Integration workflow with workflowId=%s not found".formatted(
                        oldIntegrationInstanceConfigurationWorkflow.getWorkflowId()),
                    IntegrationWorkflowErrorType.INTEGRATION_WORKFLOW_NOT_FOUND));

            String workflowId = allIntegrationWorkflows.stream()
                .filter(curIntegrationWorkflow -> Objects.equals(
                    curIntegrationWorkflow.getUuidAsString(), workflowUuid) &&
                    curIntegrationWorkflow.getIntegrationVersion() == integrationInstanceConfiguration
                        .getIntegrationVersion())
                .findFirst()
                .map(IntegrationWorkflow::getWorkflowId)
                .orElse(null);

            if (workflowId == null || CollectionUtils.noneMatch(
                integrationInstanceConfigurationWorkflows,
                integrationInstanceConfigurationWorkflow -> Objects.equals(
                    integrationInstanceConfigurationWorkflow.getWorkflowId(), workflowId))) {

                if (oldIntegrationInstanceConfigurationWorkflow.isEnabled()) {
                    doEnableIntegrationInstanceConfigurationWorkflow(
                        integrationInstanceConfiguration.getId(),
                        oldIntegrationInstanceConfigurationWorkflow.getWorkflowId(), false);
                }

                integrationInstanceWorkflowService.deleteByIntegrationInstanceConfigurationWorkflowId(
                    oldIntegrationInstanceConfigurationWorkflow.getId());

                integrationInstanceConfigurationWorkflowService.delete(
                    oldIntegrationInstanceConfigurationWorkflow.getId());
            }
        }
    }

    private List<Tag> checkTags(List<Tag> tags) {
        return CollectionUtils.isEmpty(tags) ? Collections.emptyList() : tagService.save(tags);
    }

    private static boolean containsTag(IntegrationInstanceConfiguration integrationInstanceConfiguration, Tag tag) {
        List<Long> tagIds = integrationInstanceConfiguration.getTagIds();

        return tagIds.contains(tag.getId());
    }

    private IntegrationInstanceConfigurationWorkflow doEnableIntegrationInstanceConfigurationWorkflow(
        long integrationInstanceConfigurationId, String workflowId, boolean enable) {

        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                integrationInstanceConfigurationId, workflowId);

        if (enable) {
            Integration integration = integrationService.getIntegrationInstanceConfigurationIntegration(
                integrationInstanceConfigurationId);
            Workflow workflow = workflowService.getWorkflow(workflowId);

            List<ComponentConnection> requiredComponentConnections = CollectionUtils.concat(
                WorkflowTrigger.of(workflow)
                    .stream()
                    .flatMap(workflowTrigger -> CollectionUtils.stream(
                        componentConnectionFacade.getComponentConnections(workflowTrigger)))
                    .filter(ComponentConnection::required)
                    .toList(),
                workflow.getTasks(true)
                    .stream()
                    .flatMap(workflowTask -> CollectionUtils.stream(
                        componentConnectionFacade.getComponentConnections(workflowTask)))
                    .filter(ComponentConnection::required)
                    .toList());

            requiredComponentConnections = requiredComponentConnections.stream()
                .filter(workflowConnection -> !Objects.equals(
                    workflowConnection.componentName(), integration.getComponentName()))
                .toList();

            List<String> workflowNodeNames = requiredComponentConnections.stream()
                .map(ComponentConnection::workflowNodeName)
                .toList();

            List<IntegrationInstanceConfigurationWorkflowConnection> connections =
                integrationInstanceConfigurationWorkflow.getConnections()
                    .stream()
                    .filter(connection -> workflowNodeNames.contains(connection.getWorkflowNodeName()))
                    .toList();

            if (!requiredComponentConnections.isEmpty() && requiredComponentConnections.size() != connections.size()) {
                throw new ConfigurationException(
                    "Not all required connections are set for a workflow with id=%s".formatted(workflow.getId()),
                    IntegrationInstanceConfigurationErrorType.WORKFLOW_CONNECTIONS_NOT_FOUND);
            }
        }

        List<IntegrationInstance> integrationInstances = integrationInstanceService.getIntegrationInstances(
            integrationInstanceConfigurationId);

        for (IntegrationInstance integrationInstance : integrationInstances) {
            IntegrationInstanceConfiguration integrationInstanceConfiguration =
                integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
                    integrationInstance.getIntegrationInstanceConfigurationId());

            if (!integrationInstanceConfiguration.isEnabled()) {
                continue;
            }

            ConnectedUser connectedUser = connectedUserService.getConnectedUser(
                integrationInstance.getConnectedUserId());

            if (!connectedUser.isEnabled()) {
                continue;
            }

            boolean enabled = integrationInstanceWorkflowService
                .fetchIntegrationInstanceWorkflow(integrationInstance.getId(), workflowId)
                .map(IntegrationInstanceWorkflow::isEnabled)
                .orElse(false);

            if (enabled) {
                integrationInstanceFacade.enableIntegrationInstanceWorkflowTriggers(
                    integrationInstance.getId(), workflowId, enable);
            }
        }

        return integrationInstanceConfigurationWorkflow;
    }

    private List<Tag> filterTags(List<Tag> tags, IntegrationInstanceConfiguration integrationInstanceConfiguration) {
        return CollectionUtils.filter(tags, tag -> containsTag(integrationInstanceConfiguration, tag));
    }

    private Category getCategory(Integration integration) {
        return integration.getCategoryId() == null ? null : categoryService.getCategory(integration.getCategoryId());
    }

    private static Map<String, ?> getConnectionAuthorizationParameters(
        Map<String, ?> parameters, List<String> authorizationPropertyNames) {

        return parameters.entrySet()
            .stream()
            .filter(entry -> authorizationPropertyNames.contains(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static Map<String, ?> getConnectionConnectionParameters(
        Map<String, ?> parameters, List<String> connectionPropertyNames) {

        return parameters.entrySet()
            .stream()
            .filter(entry -> connectionPropertyNames.contains(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private List<IntegrationDTO> getIntegrations(
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

    private List<Long> getIntegrationWorkflowIds(Integration integration) {
        return integrationWorkflowService.getIntegrationWorkflowIds(integration.getId(),
            integration.getLastIntegrationVersion());
    }

    private List<Tag> getTags(List<IntegrationInstanceConfiguration> integrationInstanceConfigurations) {
        return tagService.getTags(
            integrationInstanceConfigurations
                .stream()
                .flatMap(integrationInstanceConfiguration -> CollectionUtils.stream(
                    integrationInstanceConfiguration.getTagIds()))
                .filter(Objects::nonNull)
                .toList());
    }

    private List<String> getWorkflowIds(IntegrationInstanceConfiguration integrationInstanceConfiguration) {
        return integrationInstanceConfiguration.getIntegrationVersion() == null
            ? List.of()
            : integrationWorkflowService.getWorkflowIds(
                integrationInstanceConfiguration.getIntegrationId(),
                integrationInstanceConfiguration.getIntegrationVersion());
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

    private String getWorkflowUuid(
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

    private IntegrationInstanceConfigurationDTO toIntegrationInstanceConfigurationDTO(
        IntegrationInstanceConfiguration integrationInstanceConfiguration,
        List<IntegrationInstanceConfigurationWorkflow> integrationInstanceWorkflows,
        List<IntegrationWorkflow> integrationWorkflows) {

        return toIntegrationInstanceConfigurationDTO(
            toIntegrationDTO(integrationService.getIntegration(integrationInstanceConfiguration.getIntegrationId())),
            integrationInstanceConfiguration, integrationInstanceWorkflows, integrationWorkflows,
            tagService.getTags(integrationInstanceConfiguration.getTagIds()));
    }

    private IntegrationInstanceConfigurationDTO toIntegrationInstanceConfigurationDTO(
        IntegrationDTO integrationDTO, IntegrationInstanceConfiguration integrationInstanceConfiguration,
        List<IntegrationInstanceConfigurationWorkflow> integrationInstanceWorkflows,
        List<IntegrationWorkflow> integrationWorkflows, List<Tag> tags) {

        // TODO define scenarios when there are multiple component versions and workflows

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
            getConnectionAuthorizationParameters(
                integrationInstanceConfiguration.getConnectionParameters(), authorizationPropertyNames),
            getConnectionConnectionParameters(
                integrationInstanceConfiguration.getConnectionParameters(), connectionPropertyNames),
            integrationInstanceConfiguration,
            CollectionUtils.map(
                integrationInstanceWorkflows,
                integrationInstanceConfigurationWorkflow -> {
                    String workflowUuid = getWorkflowUuid(
                        integrationInstanceConfigurationWorkflow.getWorkflowId(),
                        integrationInstanceConfiguration.getIntegrationVersion(), integrationWorkflows);

                    List<String> workflowUuidWorkflowIds = integrationWorkflows.stream()
                        .filter(projectWorkflow -> Objects.equals(
                            projectWorkflow.getUuidAsString(), workflowUuid))
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
            tags);
    }

    private IntegrationDTO toIntegrationDTO(Integration integration) {
        return new IntegrationDTO(
            getCategory(integration),
            componentDefinitionService.getComponentDefinition(integration.getComponentName(), null),
            integration, getIntegrationWorkflowIds(integration),
            tagService.getTags(integration.getTagIds()));
    }

    private void validateIntegrationInstanceConfigurationWorkflow(
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow) {

        if (integrationInstanceConfigurationWorkflow.isEnabled()) {
            List<IntegrationInstanceConfigurationWorkflowConnection> integrationInstanceConfigurationWorkflowConnections =
                integrationInstanceConfigurationWorkflow.getConnections();
            Workflow workflow = workflowService.getWorkflow(integrationInstanceConfigurationWorkflow.getWorkflowId());

            validateIntegrationInstanceConfigurationWorkflowConnections(
                integrationInstanceConfigurationWorkflowConnections, workflow);
//            validateIntegrationInstanceConfigurationWorkflowInputs(
//                integrationInstanceConfigurationWorkflow.getInputs(), workflow);
        }
    }

    private void validateIntegrationInstanceConfigurationWorkflowConnections(
        List<IntegrationInstanceConfigurationWorkflowConnection> integrationInstanceConfigurationWorkflowConnections,
        Workflow workflow) {

        for (IntegrationInstanceConfigurationWorkflowConnection integrationInstanceConfigurationWorkflowConnection : integrationInstanceConfigurationWorkflowConnections) {

            Connection connection = connectionService.getConnection(
                integrationInstanceConfigurationWorkflowConnection.getConnectionId());

            ComponentConnection componentConnection = componentConnectionFacade.getComponentConnection(
                workflow.getId(), integrationInstanceConfigurationWorkflowConnection.getWorkflowNodeName(),
                integrationInstanceConfigurationWorkflowConnection.getWorkflowConnectionKey());

            if (!Objects.equals(connection.getComponentName(), componentConnection.componentName())) {
                throw new ConfigurationException(
                    "Connection component name does not match workflow connection component name",
                    ConnectionErrorType.INVALID_CONNECTION_COMPONENT_NAME);
            }
        }
    }

//    private void validateIntegrationInstanceConfigurationWorkflowInputs(Map<String, ?> inputs, Workflow workflow) {
//        for (Workflow.Input input : workflow.getInputs()) {
//            if (input.required()) {
//                Assert.isTrue(inputs.containsKey(input.name()), "Missing required param: " + input.name());
//                Assert.hasText((String) inputs.get(input.name()), "Missing required param: " + input.name());
//            }
//        }
//    }
}
