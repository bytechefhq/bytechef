/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.facade.JobFacade;
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
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.exception.ConnectionErrorType;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.springframework.security.access.prepost.PreAuthorize;
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

    private final ConnectedUserService connectedUserService;
    private final ConnectionService connectionService;
    private final EnvironmentService environmentService;
    private final PrincipalJobFacade principalJobFacade;
    private final PrincipalJobService principalJobService;
    private final JobFacade jobFacade;
    private final IntegrationInstanceConfigurationDTOAssembler integrationInstanceConfigurationDTOAssembler;
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
        ConnectedUserService connectedUserService, ConnectionService connectionService,
        EnvironmentService environmentService, PrincipalJobFacade principalJobFacade,
        PrincipalJobService principalJobService, JobFacade jobFacade,
        IntegrationInstanceConfigurationDTOAssembler integrationInstanceConfigurationDTOAssembler,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationInstanceFacade integrationInstanceFacade, IntegrationInstanceService integrationInstanceService,
        IntegrationInstanceWorkflowService integrationInstanceWorkflowService, IntegrationService integrationService,
        IntegrationWorkflowService integrationWorkflowService, OAuth2Service oAuth2Service, TagService tagService,
        TriggerExecutionService triggerExecutionService, ComponentConnectionFacade componentConnectionFacade,
        WorkflowService workflowService) {

        this.connectedUserService = connectedUserService;
        this.connectionService = connectionService;
        this.environmentService = environmentService;
        this.principalJobFacade = principalJobFacade;
        this.principalJobService = principalJobService;
        this.jobFacade = jobFacade;
        this.integrationInstanceConfigurationDTOAssembler = integrationInstanceConfigurationDTOAssembler;
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
    @PreAuthorize("isTenantAdmin()")
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
            .anyMatch(curIntegrationInstanceConfiguration -> Objects.equals(
                curIntegrationInstanceConfiguration.getIntegrationVersion(), integrationVersion))) {

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
    @PreAuthorize("isTenantAdmin()")
    public long createIntegrationInstanceConfigurationWorkflowJob(Long id, String workflowId) {
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(id, workflowId);

        return principalJobFacade.createJob(
            new JobParametersDTO(workflowId, integrationInstanceConfigurationWorkflow.getInputs()), id,
            PlatformType.EMBEDDED);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
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
        }

        List<Long> orderedJobIds = jobIds.stream()
            .distinct()
            .sorted(Comparator.reverseOrder())
            .toList();

        for (long jobId : orderedJobIds) {
            jobFacade.deleteJob(jobId);
        }

        List<IntegrationInstance> integrationInstances = integrationInstanceService.getIntegrationInstances(id);

        for (IntegrationInstance integrationInstance : integrationInstances) {
            integrationInstanceFacade.deleteIntegrationInstance(integrationInstance.getId());
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
    @PreAuthorize("isTenantAdmin()")
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
    @PreAuthorize("isTenantAdmin()")
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
    @PreAuthorize("isAuthenticated()")
    public List<IntegrationInstanceConfigurationDTO> getIntegrationInstanceConfigurationIntegrations(
        boolean enabled, Environment environment) {

        List<IntegrationInstanceConfigurationDTO> integrationInstanceConfigurationDTOs = List.of();

        Map<Long, Optional<IntegrationInstanceConfiguration>> selfServeConfigurationsByIntegrationId =
            integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(environment, true)
                .stream()
                .collect(
                    Collectors.groupingBy(
                        IntegrationInstanceConfiguration::getIntegrationId, LinkedHashMap::new,
                        Collectors.maxBy(IntegrationInstanceConfigurationServiceImpl.SELF_SERVE_ORDER)));

        List<IntegrationInstanceConfiguration> integrationInstanceConfigurations =
            selfServeConfigurationsByIntegrationId.values()
                .stream()
                .flatMap(Optional::stream)
                .toList();

        if (!integrationInstanceConfigurations.isEmpty()) {
            List<IntegrationDTO> integrationDTOs =
                integrationInstanceConfigurationDTOAssembler.getIntegrationDTOs(integrationInstanceConfigurations);
            List<Tag> tags = integrationInstanceConfigurationDTOAssembler.getTags(integrationInstanceConfigurations);

            integrationInstanceConfigurationDTOs = integrationInstanceConfigurations.stream()
                .map(integrationInstanceConfiguration -> {
                    IntegrationDTO integrationDTO = integrationDTOs.stream()
                        .filter(curIntegration -> Objects.equals(
                            curIntegration.id(), integrationInstanceConfiguration.getIntegrationId()))
                        .findFirst()
                        .orElseThrow();

                    return integrationInstanceConfigurationDTOAssembler
                        .toConnectedUserIntegrationInstanceConfigurationDTO(
                            integrationDTO, integrationInstanceConfiguration, tags);
                })
                .toList();
        }

        return integrationInstanceConfigurationDTOs;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("isTenantAdmin()")
    public IntegrationInstanceConfigurationDTO getIntegrationInstanceConfiguration(long id) {
        IntegrationInstanceConfiguration integrationInstanceConfiguration = integrationInstanceConfigurationService
            .getIntegrationInstanceConfiguration(id);

        List<IntegrationWorkflow> integrationWorkflows = integrationWorkflowService.getIntegrationWorkflows(
            integrationInstanceConfiguration.getIntegrationId(),
            integrationInstanceConfiguration.getIntegrationVersion());
        List<String> workflowIds = getWorkflowIds(integrationInstanceConfiguration);

        return integrationInstanceConfigurationDTOAssembler.toIntegrationInstanceConfigurationDTO(
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
    @PreAuthorize("isTenantAdmin()")
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
    @PreAuthorize("isTenantAdmin()")
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
            List<IntegrationDTO> integrationDTOs =
                integrationInstanceConfigurationDTOAssembler.getIntegrationDTOs(integrationInstanceConfigurations);
            List<Tag> tags = integrationInstanceConfigurationDTOAssembler.getTags(integrationInstanceConfigurations);

            List<Long> integrationIds = integrationInstanceConfigurations.stream()
                .map(IntegrationInstanceConfiguration::getIntegrationId)
                .distinct()
                .toList();

            List<IntegrationWorkflow> allIntegrationWorkflows =
                integrationWorkflowService.getIntegrationWorkflows(integrationIds);

            return CollectionUtils.map(
                integrationInstanceConfigurations,
                integrationInstanceConfiguration -> {
                    Integer integrationVersion = integrationInstanceConfiguration.getIntegrationVersion();

                    List<IntegrationWorkflow> configIntegrationWorkflows =
                        integrationVersion == null
                            ? List.of()
                            : allIntegrationWorkflows.stream()
                                .filter(
                                    integrationWorkflow -> Objects.equals(
                                        integrationWorkflow.getIntegrationId(),
                                        integrationInstanceConfiguration.getIntegrationId()) &&
                                        integrationWorkflow.getIntegrationVersion() == integrationVersion)
                                .toList();

                    List<String> workflowIds = configIntegrationWorkflows.stream()
                        .map(IntegrationWorkflow::getWorkflowId)
                        .toList();

                    return integrationInstanceConfigurationDTOAssembler.toIntegrationInstanceConfigurationDTO(
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
                        configIntegrationWorkflows,
                        tags);
                });
        } else {
            return CollectionUtils.map(integrationInstanceConfigurations, IntegrationInstanceConfigurationDTO::new);
        }
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
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
    @PreAuthorize("isTenantAdmin()")
    public void updateIntegrationInstanceConfigurationTags(long id, List<Tag> tags) {
        integrationInstanceConfigurationService.update(id, CollectionUtils.map(checkTags(tags), Tag::getId));
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
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
                    if (integrationInstanceConfiguration.isEnabled() &&
                        oldIntegrationInstanceConfigurationWorkflowEnabled) {

                        doEnableIntegrationInstanceConfigurationWorkflow(
                            integrationInstanceConfiguration.getId(), oldWorkflowId, false);
                    }

                    integrationInstanceConfigurationWorkflowService.update(oldIntegrationInstanceConfigurationWorkflow);

                    if (integrationInstanceConfiguration.isEnabled()) {
                        doEnableIntegrationInstanceConfigurationWorkflow(
                            integrationInstanceConfiguration.getId(),
                            integrationInstanceConfigurationWorkflow.getWorkflowId(), true);
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

    private List<String> getWorkflowIds(IntegrationInstanceConfiguration integrationInstanceConfiguration) {
        return integrationInstanceConfiguration.getIntegrationVersion() == null
            ? List.of()
            : integrationWorkflowService.getWorkflowIds(
                integrationInstanceConfiguration.getIntegrationId(),
                integrationInstanceConfiguration.getIntegrationVersion());
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
