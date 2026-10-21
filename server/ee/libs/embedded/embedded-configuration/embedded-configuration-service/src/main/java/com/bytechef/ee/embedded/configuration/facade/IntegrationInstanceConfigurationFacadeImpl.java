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
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.audit.IntegrationAuditEvents;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflowConnection;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationVersionImpactDTO;
import com.bytechef.ee.embedded.configuration.dto.ConfigurationMoveImpactDTO;
import com.bytechef.ee.embedded.configuration.dto.ConfigurationMoveResultDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationConnectedUserDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.exception.IntegrationInstanceConfigurationErrorType;
import com.bytechef.ee.embedded.configuration.exception.IntegrationWorkflowErrorType;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
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
import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.Audited;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class IntegrationInstanceConfigurationFacadeImpl implements IntegrationInstanceConfigurationFacade {

    private static final Logger log = LoggerFactory.getLogger(IntegrationInstanceConfigurationFacadeImpl.class);

    private static final String INSTANCE_CONFIGURATION_EDIT = "INTEGRATION_INSTANCE_CONFIGURATION_EDIT";
    private static final String INSTANCE_CONFIGURATION_TYPE = "IntegrationInstanceConfiguration";
    private static final String INSTANCE_CONFIGURATION_VIEW = "INTEGRATION_INSTANCE_CONFIGURATION_VIEW";

    private final ConnectedUserService connectedUserService;
    private final ConnectionService connectionService;
    private final EmbeddedPermissionEvaluator embeddedPermissionEvaluator;
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
    private final McpIntegrationInstanceConfigurationService mcpIntegrationInstanceConfigurationService;
    private final OAuth2Service oAuth2Service;
    private final TagService tagService;
    private final TransactionTemplate requiresNewTransactionTemplate;
    private final TriggerExecutionService triggerExecutionService;
    private final ComponentConnectionFacade componentConnectionFacade;
    private final WorkflowService workflowService;
    private final PermissionService permissionService;

    @SuppressFBWarnings("EI")
    public IntegrationInstanceConfigurationFacadeImpl(
        ConnectedUserService connectedUserService, ConnectionService connectionService,
        EmbeddedPermissionEvaluator embeddedPermissionEvaluator, EnvironmentService environmentService,
        PlatformTransactionManager platformTransactionManager, PrincipalJobFacade principalJobFacade,
        PrincipalJobService principalJobService, JobFacade jobFacade,
        IntegrationInstanceConfigurationDTOAssembler integrationInstanceConfigurationDTOAssembler,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationInstanceFacade integrationInstanceFacade, IntegrationInstanceService integrationInstanceService,
        IntegrationInstanceWorkflowService integrationInstanceWorkflowService, IntegrationService integrationService,
        IntegrationWorkflowService integrationWorkflowService,
        McpIntegrationInstanceConfigurationService mcpIntegrationInstanceConfigurationService,
        OAuth2Service oAuth2Service,
        TagService tagService,
        TriggerExecutionService triggerExecutionService, ComponentConnectionFacade componentConnectionFacade,
        WorkflowService workflowService, @Lazy PermissionService permissionService) {

        this.connectedUserService = connectedUserService;
        this.connectionService = connectionService;
        this.embeddedPermissionEvaluator = embeddedPermissionEvaluator;
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
        this.mcpIntegrationInstanceConfigurationService = mcpIntegrationInstanceConfigurationService;
        this.oAuth2Service = oAuth2Service;
        this.tagService = tagService;
        this.triggerExecutionService = triggerExecutionService;
        this.componentConnectionFacade = componentConnectionFacade;
        this.workflowService = workflowService;

        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);

        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        this.requiresNewTransactionTemplate = transactionTemplate;
        this.permissionService = permissionService;
    }

    @Override
    @Audited(event = IntegrationAuditEvents.INTEGRATION_INSTANCE_CONFIGURATION_CREATED, data = {
        @AuditData(key = "integrationInstanceConfigurationId", value = "#result"),
        @AuditData(key = "integrationId", value = "#integrationInstanceConfigurationDTO.integrationId"),
        @AuditData(key = "name", value = "#integrationInstanceConfigurationDTO.name"),
        @AuditData(key = "environment", value = "#integrationInstanceConfigurationDTO.environment")
    })
    @PreAuthorize("hasEmbeddedScopeInEnvironment('INTEGRATION_INSTANCE_CONFIGURATION_CREATE', " +
        "#integrationInstanceConfigurationDTO.environment)")
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
    @Audited(event = IntegrationAuditEvents.INTEGRATION_INSTANCE_CONFIGURATION_WORKFLOW_RUN, data = {
        @AuditData(key = "integrationInstanceConfigurationId", value = "#id"),
        @AuditData(key = "workflowId", value = "#workflowId"),
        @AuditData(key = "jobId", value = "#result")
    })
    @Transactional(propagation = Propagation.NEVER)
    @PreAuthorize("hasPermission(#id, 'IntegrationInstanceConfiguration', 'INTEGRATION_INSTANCE_CONFIGURATION_EDIT')")
    public long createIntegrationInstanceConfigurationWorkflowJob(Long id, String workflowId) {
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(id, workflowId);

        return principalJobFacade.createJob(
            new JobParametersDTO(workflowId, integrationInstanceConfigurationWorkflow.getInputs()), id,
            PlatformType.EMBEDDED);
    }

    @Override
    @Audited(event = IntegrationAuditEvents.INTEGRATION_INSTANCE_CONFIGURATION_DELETED, data = {
        @AuditData(key = "integrationInstanceConfigurationId", value = "#id")
    })
    @PreAuthorize("hasPermission(#id, 'IntegrationInstanceConfiguration', 'INTEGRATION_INSTANCE_CONFIGURATION_DELETE')")
    public void deleteIntegrationInstanceConfiguration(long id) {
        IntegrationInstanceConfiguration integrationInstanceConfiguration = integrationInstanceConfigurationService
            .getIntegrationInstanceConfiguration(id);

        ConfigurationDeletionGuard.assertMayDelete(
            List.of(integrationInstanceConfiguration), integrationInstanceService, permissionService);

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
    }

    @Override
    @Audited(event = IntegrationAuditEvents.INTEGRATION_INSTANCE_CONFIGURATION_ENABLED_UPDATED, data = {
        @AuditData(key = "integrationInstanceConfigurationId", value = "#integrationInstanceConfigurationId"),
        @AuditData(key = "enabled", value = "#enable")
    })
    @PreAuthorize("hasPermission(#integrationInstanceConfigurationId, " +
        "'IntegrationInstanceConfiguration', 'INTEGRATION_INSTANCE_CONFIGURATION_EDIT')")
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
    @Audited(event = IntegrationAuditEvents.INTEGRATION_INSTANCE_CONFIGURATION_WORKFLOW_ENABLED_UPDATED, data = {
        @AuditData(key = "integrationInstanceConfigurationId", value = "#integrationInstanceConfigurationId"),
        @AuditData(key = "workflowId", value = "#workflowId"),
        @AuditData(key = "enabled", value = "#enable")
    })
    @PreAuthorize("hasPermission(#integrationInstanceConfigurationId, " +
        "'IntegrationInstanceConfiguration', 'INTEGRATION_INSTANCE_CONFIGURATION_EDIT')")
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
        long integrationId, boolean enabled, Environment environment, @Nullable ConnectedUser connectedUser) {

        return getIntegrationInstanceConfigurationIntegrations(true, environment, connectedUser)
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
        boolean enabled, Environment environment, @Nullable ConnectedUser connectedUser) {

        List<IntegrationInstanceConfigurationDTO> integrationInstanceConfigurationDTOs = List.of();

        List<IntegrationInstanceConfiguration> enabledIntegrationInstanceConfigurations =
            integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(environment, true);

        Map<Long, Optional<IntegrationInstanceConfiguration>> selfServeConfigurationsByIntegrationId =
            enabledIntegrationInstanceConfigurations.stream()
                .filter(
                    integrationInstanceConfiguration -> connectedUser == null || embeddedPermissionEvaluator.evaluate(
                        integrationInstanceConfiguration.getPermissionExpression(), connectedUser))
                .collect(
                    Collectors.groupingBy(
                        IntegrationInstanceConfiguration::getIntegrationId, LinkedHashMap::new,
                        Collectors.maxBy(IntegrationInstanceConfigurationServiceImpl.SELF_SERVE_ORDER)));

        if (connectedUser != null) {
            addConfigurationsOfExistingInstances(
                selfServeConfigurationsByIntegrationId, enabledIntegrationInstanceConfigurations, connectedUser,
                environment);
        }

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
    @PreAuthorize("hasPermission(#id, 'IntegrationInstanceConfiguration', 'INTEGRATION_INSTANCE_CONFIGURATION_VIEW')")
    public IntegrationInstanceConfigurationDTO getIntegrationInstanceConfiguration(long id) {
        IntegrationInstanceConfiguration integrationInstanceConfiguration = integrationInstanceConfigurationService
            .getIntegrationInstanceConfiguration(id);

        List<IntegrationWorkflow> integrationWorkflows = integrationWorkflowService.getIntegrationWorkflows(
            integrationInstanceConfiguration.getIntegrationId(),
            integrationInstanceConfiguration.getIntegrationVersion());
        List<String> workflowIds = getWorkflowIds(integrationInstanceConfiguration);

        IntegrationInstanceConfigurationDTO integrationInstanceConfigurationDTO =
            integrationInstanceConfigurationDTOAssembler.toIntegrationInstanceConfigurationDTO(
                integrationInstanceConfiguration,
                CollectionUtils.filter(
                    integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(
                        integrationInstanceConfiguration.getId()),
                    integrationInstanceConfigurationWorkflow -> workflowIds
                        .contains(integrationInstanceConfigurationWorkflow.getWorkflowId())),
                integrationWorkflows);

        return permissionService.hasResourceScope(id, INSTANCE_CONFIGURATION_TYPE, INSTANCE_CONFIGURATION_EDIT)
            ? integrationInstanceConfigurationDTO : integrationInstanceConfigurationDTO.withoutCredentials();
    }

    private List<IntegrationInstanceConfigurationDTO> redactCredentials(
        List<IntegrationInstanceConfigurationDTO> integrationInstanceConfigurationDTOs) {

        Map<Environment, Boolean> canEditByEnvironment = new HashMap<>();
        List<IntegrationInstanceConfigurationDTO> redactedDTOs = new ArrayList<>();

        for (IntegrationInstanceConfigurationDTO integrationInstanceConfigurationDTO : integrationInstanceConfigurationDTOs) {

            boolean canEdit = canEditByEnvironment.computeIfAbsent(
                integrationInstanceConfigurationDTO.environment(),
                environment -> permissionService.hasResourceScope(
                    integrationInstanceConfigurationDTO.id(), INSTANCE_CONFIGURATION_TYPE,
                    INSTANCE_CONFIGURATION_EDIT));

            redactedDTOs.add(
                canEdit
                    ? integrationInstanceConfigurationDTO : integrationInstanceConfigurationDTO.withoutCredentials());
        }

        return redactedDTOs;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#id, 'IntegrationInstanceConfiguration', 'INTEGRATION_INSTANCE_CONFIGURATION_VIEW')")
    public List<IntegrationInstanceConfigurationConnectedUserDTO> getIntegrationInstanceConfigurationConnectedUsers(
        long id) {

        integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(id);

        List<Long> connectedUserIds = integrationInstanceService.getIntegrationInstances(id)
            .stream()
            .map(IntegrationInstance::getConnectedUserId)
            .distinct()
            .sorted()
            .toList();

        if (connectedUserIds.isEmpty()) {
            return List.of();
        }

        return connectedUserService.getConnectedUsers(connectedUserIds)
            .stream()
            .sorted(Comparator.comparing(ConnectedUser::getId))
            .map(
                connectedUser -> new IntegrationInstanceConfigurationConnectedUserDTO(
                    connectedUser.getId(), connectedUser.getExternalId(), connectedUser.getName(),
                    connectedUser.getEmail()))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#sourceId, 'IntegrationInstanceConfiguration', 'INTEGRATION_INSTANCE_CONFIGURATION_VIEW') and "
        + "hasPermission(#targetId, 'IntegrationInstanceConfiguration', 'INTEGRATION_INSTANCE_CONFIGURATION_VIEW')")
    public ConfigurationMoveImpactDTO getMoveImpact(long sourceId, long targetId, List<Long> connectedUserIds) {
        IntegrationInstanceConfiguration sourceConfiguration =
            integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(sourceId);
        IntegrationInstanceConfiguration targetConfiguration =
            integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(targetId);

        validateSameIntegrationAndEnvironment(sourceConfiguration, targetConfiguration);

        MoveEvaluation moveEvaluation = evaluateMove(
            getMoveContext(sourceConfiguration, targetConfiguration), getInstancesByConnectedUserId(sourceId),
            getSortedDistinct(connectedUserIds));

        return new ConfigurationMoveImpactDTO(
            getMoveImpactWorkflows(moveEvaluation.movingWorkflowMappings(), targetConfiguration),
            moveEvaluation.skipped(), getMcpServerCount(List.of(sourceConfiguration, targetConfiguration)));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasEmbeddedScope('INTEGRATION_INSTANCE_CONFIGURATION_VIEW')")
    public List<Tag> getIntegrationInstanceConfigurationTags() {
        List<IntegrationInstanceConfiguration> integrationInstanceConfigurations =
            integrationInstanceConfigurationService.getIntegrationInstanceConfigurations();

        Set<Environment> viewableEnvironments = Arrays.stream(Environment.values())
            .filter(environment -> permissionService.hasEmbeddedScope(INSTANCE_CONFIGURATION_VIEW, environment))
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(Environment.class)));

        return tagService.getTags(
            integrationInstanceConfigurations
                .stream()
                .filter(integrationInstanceConfiguration -> viewableEnvironments.contains(
                    integrationInstanceConfiguration.getEnvironment()))
                .map(IntegrationInstanceConfiguration::getTagIds)
                .flatMap(Collection::stream)
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasEmbeddedScopeInEnvironmentId('INTEGRATION_INSTANCE_CONFIGURATION_VIEW', #environmentId)")
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

            return redactCredentials(CollectionUtils.map(
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
                }));
        } else {
            return redactCredentials(
                CollectionUtils.map(integrationInstanceConfigurations, IntegrationInstanceConfigurationDTO::new));
        }
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#targetId, 'IntegrationInstanceConfiguration', 'INTEGRATION_INSTANCE_CONFIGURATION_VIEW')")
    public ConfigurationMoveImpactDTO getUpgradeImpact(long targetId, List<Long> connectedUserIds) {
        IntegrationInstanceConfiguration targetConfiguration =
            integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(targetId);

        List<Long> sortedConnectedUserIds = getSortedDistinct(connectedUserIds);

        List<UpgradeSource> upgradeSources = getUpgradeSources(targetConfiguration, sortedConnectedUserIds);

        List<IntegrationInstanceConfiguration> exposedConfigurations = new ArrayList<>();
        Set<Long> movingConnectedUserIds = new HashSet<>();
        List<WorkflowMapping> movingWorkflowMappings = new ArrayList<>();
        List<ConfigurationMoveResultDTO.Skipped> skipped = new ArrayList<>();

        exposedConfigurations.add(targetConfiguration);

        for (UpgradeSource upgradeSource : upgradeSources) {
            MoveEvaluation moveEvaluation = evaluateMove(
                upgradeSource.moveContext(), upgradeSource.instancesByConnectedUserId(),
                upgradeSource.connectedUserIds());

            exposedConfigurations.add(
                upgradeSource.moveContext()
                    .sourceConfiguration());
            movingConnectedUserIds.addAll(moveEvaluation.movingConnectedUserIds());
            movingWorkflowMappings.addAll(moveEvaluation.movingWorkflowMappings());
            skipped.addAll(moveEvaluation.skipped());
        }

        skipped.addAll(getUpgradeUnmatchedSkipped(targetConfiguration, upgradeSources, sortedConnectedUserIds));

        return new ConfigurationMoveImpactDTO(
            getMoveImpactWorkflows(movingWorkflowMappings, targetConfiguration),
            mergeUpgradeSkipped(skipped, movingConnectedUserIds), getMcpServerCount(exposedConfigurations));
    }

    @Override
    @Audited(event = IntegrationAuditEvents.INTEGRATION_INSTANCE_CONFIGURATION_CONNECTED_USERS_MOVED, data = {
        @AuditData(key = "sourceIntegrationInstanceConfigurationId", value = "#sourceId"),
        @AuditData(key = "targetIntegrationInstanceConfigurationId", value = "#targetId"),
        @AuditData(key = "connectedUserIds", value = "#connectedUserIds")
    })
    @PreAuthorize("hasPermission(#sourceId, 'IntegrationInstanceConfiguration', 'INTEGRATION_INSTANCE_CONFIGURATION_EDIT') and "
        + "hasPermission(#targetId, 'IntegrationInstanceConfiguration', 'INTEGRATION_INSTANCE_CONFIGURATION_EDIT')")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ConfigurationMoveResultDTO moveConnectedUsers(long sourceId, long targetId, List<Long> connectedUserIds) {
        if (sourceId == targetId) {
            throw new IllegalArgumentException("The source and target configurations must differ");
        }

        IntegrationInstanceConfiguration sourceConfiguration =
            integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(sourceId);
        IntegrationInstanceConfiguration targetConfiguration =
            integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(targetId);

        validateSameIntegrationAndEnvironment(sourceConfiguration, targetConfiguration);

        return moveConnectedUsers(
            getMoveContext(sourceConfiguration, targetConfiguration), getInstancesByConnectedUserId(sourceId),
            getSortedDistinct(connectedUserIds));
    }

    @Override
    @Audited(event = IntegrationAuditEvents.INTEGRATION_INSTANCE_CONFIGURATION_UPDATED, data = {
        @AuditData(key = "integrationInstanceConfigurationId", value = "#integrationInstanceConfigurationDTO.id"),
        @AuditData(key = "integrationId", value = "#integrationInstanceConfigurationDTO.integrationId"),
        @AuditData(key = "name", value = "#integrationInstanceConfigurationDTO.name")
    })
    @PreAuthorize("hasPermission(#integrationInstanceConfigurationDTO.id, " +
        "'IntegrationInstanceConfiguration', 'INTEGRATION_INSTANCE_CONFIGURATION_EDIT')")
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
    @PreAuthorize("hasPermission(#id, 'IntegrationInstanceConfiguration', 'INTEGRATION_INSTANCE_CONFIGURATION_EDIT')")
    public void updateIntegrationInstanceConfigurationTags(long id, List<Tag> tags) {
        integrationInstanceConfigurationService.update(id, CollectionUtils.map(checkTags(tags), Tag::getId));
    }

    @Override
    @PreAuthorize("hasPermission(#integrationInstanceConfigurationWorkflow.integrationInstanceConfigurationId, " +
        "'IntegrationInstanceConfiguration', 'INTEGRATION_INSTANCE_CONFIGURATION_EDIT')")
    public void updateIntegrationInstanceConfigurationWorkflow(
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow) {

        validateIntegrationInstanceConfigurationWorkflow(integrationInstanceConfigurationWorkflow);

        IntegrationInstanceConfigurationWorkflow previousIntegrationInstanceConfigurationWorkflow =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                Validate.notNull(integrationInstanceConfigurationWorkflow.getId(), "id"));

        IntegrationInstanceConfigurationWorkflow updatedIntegrationInstanceConfigurationWorkflow =
            integrationInstanceConfigurationWorkflowService.update(integrationInstanceConfigurationWorkflow);

        integrationInstanceFacade.updateIntegrationInstanceConfigurationWorkflowTriggers(
            previousIntegrationInstanceConfigurationWorkflow, updatedIntegrationInstanceConfigurationWorkflow);
    }

    @Override
    @Audited(event = IntegrationAuditEvents.INTEGRATION_INSTANCE_CONFIGURATION_CONNECTED_USERS_UPGRADED, data = {
        @AuditData(key = "targetIntegrationInstanceConfigurationId", value = "#targetId"),
        @AuditData(key = "connectedUserIds", value = "#connectedUserIds")
    })
    @PreAuthorize("hasPermission(#targetId, 'IntegrationInstanceConfiguration', 'INTEGRATION_INSTANCE_CONFIGURATION_EDIT')")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ConfigurationMoveResultDTO upgradeConnectedUsers(long targetId, List<Long> connectedUserIds) {
        IntegrationInstanceConfiguration targetConfiguration =
            integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(targetId);

        List<Long> sortedConnectedUserIds = getSortedDistinct(connectedUserIds);

        List<UpgradeSource> upgradeSources = getUpgradeSources(targetConfiguration, sortedConnectedUserIds);

        List<ConfigurationMoveResultDTO.Skipped> skipped = new ArrayList<>(
            getUpgradeUnmatchedSkipped(targetConfiguration, upgradeSources, sortedConnectedUserIds));
        Set<Long> movedConnectedUserIds = new TreeSet<>();
        Set<Long> needsAttentionConnectedUserIds = new TreeSet<>();

        for (UpgradeSource upgradeSource : upgradeSources) {
            ConfigurationMoveResultDTO configurationMoveResultDTO = moveConnectedUsers(
                upgradeSource.moveContext(), upgradeSource.instancesByConnectedUserId(),
                upgradeSource.connectedUserIds());

            movedConnectedUserIds.addAll(configurationMoveResultDTO.movedConnectedUserIds());
            needsAttentionConnectedUserIds.addAll(configurationMoveResultDTO.needsAttentionConnectedUserIds());
            skipped.addAll(configurationMoveResultDTO.skipped());
        }

        List<ConfigurationMoveResultDTO.Skipped> mergedSkipped = mergeUpgradeSkipped(skipped, movedConnectedUserIds);

        for (ConfigurationMoveResultDTO.Skipped userSkipped : mergedSkipped) {
            if (movedConnectedUserIds.contains(userSkipped.connectedUserId())) {
                needsAttentionConnectedUserIds.add(userSkipped.connectedUserId());
            }
        }

        return new ConfigurationMoveResultDTO(
            List.copyOf(movedConnectedUserIds), mergedSkipped, List.copyOf(needsAttentionConnectedUserIds));
    }

    private void addConfigurationsOfExistingInstances(
        Map<Long, Optional<IntegrationInstanceConfiguration>> configurationsByIntegrationId,
        List<IntegrationInstanceConfiguration> enabledIntegrationInstanceConfigurations, ConnectedUser connectedUser,
        Environment environment) {

        Map<Long, IntegrationInstanceConfiguration> enabledIntegrationInstanceConfigurationsById =
            enabledIntegrationInstanceConfigurations.stream()
                .collect(Collectors.toMap(IntegrationInstanceConfiguration::getId, configuration -> configuration));

        List<IntegrationInstance> integrationInstances = integrationInstanceService
            .getConnectedUserIntegrationInstances(Validate.notNull(connectedUser.getId(), "id"), environment)
            .stream()
            .sorted(Comparator.comparing(IntegrationInstance::getId, Comparator.reverseOrder()))
            .toList();

        for (IntegrationInstance integrationInstance : integrationInstances) {
            IntegrationInstanceConfiguration integrationInstanceConfiguration =
                enabledIntegrationInstanceConfigurationsById.get(
                    integrationInstance.getIntegrationInstanceConfigurationId());

            if (integrationInstanceConfiguration == null) {
                continue;
            }

            Optional<IntegrationInstanceConfiguration> selfServeIntegrationInstanceConfiguration =
                configurationsByIntegrationId.get(integrationInstanceConfiguration.getIntegrationId());

            if (selfServeIntegrationInstanceConfiguration == null
                || selfServeIntegrationInstanceConfiguration.isEmpty()) {
                configurationsByIntegrationId.put(
                    integrationInstanceConfiguration.getIntegrationId(),
                    Optional.of(integrationInstanceConfiguration));
            }
        }
    }

    private MoveEvaluation evaluateMove(
        MoveContext moveContext, Map<Long, List<IntegrationInstance>> sourceInstancesByConnectedUserId,
        List<Long> sortedConnectedUserIds) {

        List<Long> movingConnectedUserIds = new ArrayList<>();
        List<ConfigurationMoveResultDTO.Skipped> skipped = new ArrayList<>();
        List<WorkflowMapping> movingWorkflowMappings = new ArrayList<>();

        for (long connectedUserId : sortedConnectedUserIds) {
            List<IntegrationInstance> integrationInstances = sourceInstancesByConnectedUserId.getOrDefault(
                connectedUserId, List.of());

            if (integrationInstances.isEmpty()) {
                skipped.add(
                    new ConfigurationMoveResultDTO.Skipped(
                        connectedUserId, ConfigurationMoveResultDTO.Skipped.NOT_ON_SOURCE, List.of()));

                continue;
            }

            List<WorkflowMapping> userWorkflowMappings = new ArrayList<>();
            List<ConfigurationMoveResultDTO.Skipped> userSkipped = new ArrayList<>();
            boolean eligible = false;

            for (IntegrationInstance integrationInstance : integrationInstances) {
                InstanceEvaluation instanceEvaluation = evaluateIntegrationInstance(integrationInstance, moveContext);

                if (instanceEvaluation.skipped() == null) {
                    eligible = true;

                    userWorkflowMappings.addAll(instanceEvaluation.workflowMappings());
                } else {
                    userSkipped.add(instanceEvaluation.skipped());
                }
            }

            if (eligible) {
                movingConnectedUserIds.add(connectedUserId);
                movingWorkflowMappings.addAll(userWorkflowMappings);
            }

            addSkipped(skipped, userSkipped, eligible);
        }

        return new MoveEvaluation(movingConnectedUserIds, movingWorkflowMappings, skipped);
    }

    private ConfigurationMoveResultDTO moveConnectedUsers(
        MoveContext moveContext, Map<Long, List<IntegrationInstance>> sourceInstancesByConnectedUserId,
        List<Long> sortedConnectedUserIds) {

        IntegrationInstanceConfiguration sourceConfiguration = moveContext.sourceConfiguration();
        IntegrationInstanceConfiguration targetConfiguration = moveContext.targetConfiguration();

        List<Long> movedConnectedUserIds = new ArrayList<>();
        List<Long> needsAttentionConnectedUserIds = new ArrayList<>();
        List<ConfigurationMoveResultDTO.Skipped> skipped = new ArrayList<>();

        for (long connectedUserId : sortedConnectedUserIds) {
            List<IntegrationInstance> integrationInstances = sourceInstancesByConnectedUserId.getOrDefault(
                connectedUserId, List.of());

            if (integrationInstances.isEmpty()) {
                skipped.add(
                    new ConfigurationMoveResultDTO.Skipped(
                        connectedUserId, ConfigurationMoveResultDTO.Skipped.NOT_ON_SOURCE, List.of()));

                continue;
            }

            boolean moved = false;
            boolean needsAttention = false;
            List<ConfigurationMoveResultDTO.Skipped> userSkipped = new ArrayList<>();

            for (IntegrationInstance integrationInstance : integrationInstances) {
                InstanceMoveOutcome instanceMoveOutcome;

                try {
                    instanceMoveOutcome = moveIntegrationInstance(integrationInstance.getId(), moveContext);
                } catch (RuntimeException exception) {
                    log.error(
                        "Moving integration instance id={} of connected user id={} from configuration id={} to " +
                            "id={} failed",
                        integrationInstance.getId(), connectedUserId, sourceConfiguration.getId(),
                        targetConfiguration.getId(), exception);

                    instanceMoveOutcome = InstanceMoveOutcome.skipped(
                        connectedUserId, ConfigurationMoveResultDTO.Skipped.MOVE_FAILED, List.of());
                }

                if (instanceMoveOutcome.skipped() == null) {
                    moved = true;
                    needsAttention = needsAttention || instanceMoveOutcome.needsAttention();
                } else {
                    userSkipped.add(instanceMoveOutcome.skipped());
                }
            }

            addSkipped(skipped, userSkipped, moved);

            if (!moved) {
                continue;
            }

            movedConnectedUserIds.add(connectedUserId);

            if (needsAttention || !userSkipped.isEmpty()) {
                needsAttentionConnectedUserIds.add(connectedUserId);
            }
        }

        return new ConfigurationMoveResultDTO(movedConnectedUserIds, skipped, needsAttentionConnectedUserIds);
    }

    private List<UpgradeSource> getUpgradeSources(
        IntegrationInstanceConfiguration targetConfiguration, List<Long> sortedConnectedUserIds) {

        List<IntegrationInstanceConfiguration> integrationInstanceConfigurations =
            integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(
                targetConfiguration.getEnvironment(), targetConfiguration.getIntegrationId(), null);

        List<UpgradeSource> upgradeSources = new ArrayList<>();

        for (IntegrationInstanceConfiguration sourceConfiguration : integrationInstanceConfigurations.stream()
            .filter(configuration -> !Objects.equals(configuration.getId(), targetConfiguration.getId()))
            .sorted(Comparator.comparing(IntegrationInstanceConfiguration::getId))
            .toList()) {

            Map<Long, List<IntegrationInstance>> sourceInstancesByConnectedUserId = getInstancesByConnectedUserId(
                sourceConfiguration.getId());

            List<Long> sourceConnectedUserIds = sortedConnectedUserIds.stream()
                .filter(sourceInstancesByConnectedUserId::containsKey)
                .toList();

            if (sourceConnectedUserIds.isEmpty()) {
                continue;
            }

            upgradeSources.add(
                new UpgradeSource(
                    getMoveContext(sourceConfiguration, targetConfiguration), sourceInstancesByConnectedUserId,
                    sourceConnectedUserIds));
        }

        return upgradeSources;
    }

    private List<ConfigurationMoveResultDTO.Skipped> getUpgradeUnmatchedSkipped(
        IntegrationInstanceConfiguration targetConfiguration, List<UpgradeSource> upgradeSources,
        List<Long> sortedConnectedUserIds) {

        Set<Long> sourceConnectedUserIds = upgradeSources.stream()
            .flatMap(upgradeSource -> upgradeSource.connectedUserIds()
                .stream())
            .collect(Collectors.toSet());
        Set<Long> targetConnectedUserIds = getInstancesByConnectedUserId(targetConfiguration.getId()).keySet();

        List<ConfigurationMoveResultDTO.Skipped> unmatchedSkipped = new ArrayList<>();

        for (long connectedUserId : sortedConnectedUserIds) {
            if (sourceConnectedUserIds.contains(connectedUserId)) {
                continue;
            }

            String reason = targetConnectedUserIds.contains(connectedUserId)
                ? ConfigurationMoveResultDTO.Skipped.ALREADY_ON_TARGET
                : ConfigurationMoveResultDTO.Skipped.NOT_ON_SOURCE;

            unmatchedSkipped.add(new ConfigurationMoveResultDTO.Skipped(connectedUserId, reason, List.of()));
        }

        return unmatchedSkipped;
    }

    private static List<ConfigurationMoveResultDTO.Skipped> mergeUpgradeSkipped(
        List<ConfigurationMoveResultDTO.Skipped> skipped, Set<Long> movedConnectedUserIds) {

        return skipped.stream()
            .filter(userSkipped -> !movedConnectedUserIds.contains(userSkipped.connectedUserId()) ||
                !Objects.equals(userSkipped.reason(), ConfigurationMoveResultDTO.Skipped.NOT_ON_SOURCE))
            .distinct()
            .sorted(Comparator.comparingLong(ConfigurationMoveResultDTO.Skipped::connectedUserId))
            .toList();
    }

    private static void addSkipped(
        List<ConfigurationMoveResultDTO.Skipped> skipped, List<ConfigurationMoveResultDTO.Skipped> userSkipped,
        boolean partlyMoved) {

        if (userSkipped.isEmpty()) {
            return;
        }

        if (partlyMoved) {
            userSkipped.stream()
                .distinct()
                .forEach(skipped::add);
        } else {
            skipped.add(userSkipped.getFirst());
        }
    }

    private int getMcpServerCount(List<IntegrationInstanceConfiguration> integrationInstanceConfigurations) {
        return (int) integrationInstanceConfigurations.stream()
            .map(IntegrationInstanceConfiguration::getId)
            .flatMap(integrationInstanceConfigurationId -> mcpIntegrationInstanceConfigurationService
                .getMcpIntegrationInstanceConfigurationsByIntegrationInstanceConfigurationId(
                    integrationInstanceConfigurationId)
                .stream())
            .map(McpIntegrationInstanceConfiguration::getMcpServerId)
            .filter(Objects::nonNull)
            .distinct()
            .count();
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

            List<ComponentConnection> requiredComponentConnections = getRequiredComponentConnections(
                workflow, integration);

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

    private Map<Long, List<IntegrationInstance>>
        getInstancesByConnectedUserId(long integrationInstanceConfigurationId) {
        return integrationInstanceService.getIntegrationInstances(integrationInstanceConfigurationId)
            .stream()
            .collect(Collectors.groupingBy(IntegrationInstance::getConnectedUserId));
    }

    private MoveContext getMoveContext(
        IntegrationInstanceConfiguration sourceConfiguration, IntegrationInstanceConfiguration targetConfiguration) {

        Map<String, String> workflowUuidsByWorkflowId = new HashMap<>();

        for (IntegrationWorkflow integrationWorkflow : integrationWorkflowService.getIntegrationWorkflows(
            sourceConfiguration.getIntegrationId())) {

            workflowUuidsByWorkflowId.put(integrationWorkflow.getWorkflowId(), integrationWorkflow.getUuidAsString());
        }

        Map<String, IntegrationInstanceConfigurationWorkflow> targetWorkflowsByUuid = new HashMap<>();

        for (IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow : integrationInstanceConfigurationWorkflowService
            .getIntegrationInstanceConfigurationWorkflows(targetConfiguration.getId())) {

            String workflowUuid = workflowUuidsByWorkflowId.get(
                integrationInstanceConfigurationWorkflow.getWorkflowId());

            if (workflowUuid != null) {
                targetWorkflowsByUuid.put(workflowUuid, integrationInstanceConfigurationWorkflow);
            }
        }

        return new MoveContext(sourceConfiguration, targetConfiguration, targetWorkflowsByUuid,
            workflowUuidsByWorkflowId);
    }

    private List<WorkflowMapping>
        getWorkflowMappings(IntegrationInstance integrationInstance, MoveContext moveContext) {
        List<WorkflowMapping> workflowMappings = new ArrayList<>();

        for (IntegrationInstanceWorkflow integrationInstanceWorkflow : integrationInstanceWorkflowService
            .getIntegrationInstanceWorkflows(integrationInstance.getId())) {

            IntegrationInstanceConfigurationWorkflow sourceWorkflow =
                integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                    integrationInstanceWorkflow.getIntegrationInstanceConfigurationWorkflowId());

            String workflowUuid = moveContext.workflowUuidsByWorkflowId()
                .get(sourceWorkflow.getWorkflowId());

            workflowMappings.add(
                new WorkflowMapping(
                    integrationInstance.getConnectedUserId(), integrationInstanceWorkflow, sourceWorkflow,
                    moveContext.targetWorkflowsByUuid()
                        .get(workflowUuid),
                    workflowUuid));
        }

        return workflowMappings;
    }

    private ConfigurationMoveResultDTO.@Nullable Skipped getSkipped(
        long connectedUserId, List<WorkflowMapping> workflowMappings, MoveContext moveContext) {

        List<String> missingWorkflowLabels = workflowMappings.stream()
            .filter(workflowMapping -> workflowMapping.integrationInstanceWorkflow()
                .isEnabled())
            .filter(workflowMapping -> !workflowMapping.hasEnabledTargetWorkflow())
            .map(this::getWorkflowLabel)
            .distinct()
            .sorted()
            .toList();

        if (!missingWorkflowLabels.isEmpty()) {
            return new ConfigurationMoveResultDTO.Skipped(
                connectedUserId, ConfigurationMoveResultDTO.Skipped.TARGET_LACKS_ENABLED_WORKFLOWS,
                missingWorkflowLabels);
        }

        if (moveContext.sourceConfiguration()
            .getAuthorizationType() != moveContext.targetConfiguration()
                .getAuthorizationType()) {

            return new ConfigurationMoveResultDTO.Skipped(
                connectedUserId, ConfigurationMoveResultDTO.Skipped.CONNECTION_MISMATCH, List.of());
        }

        return null;
    }

    private String getWorkflowLabel(WorkflowMapping workflowMapping) {
        Workflow workflow = workflowService.getWorkflow(
            workflowMapping.sourceWorkflow()
                .getWorkflowId());

        return StringUtils.isBlank(workflow.getLabel()) ? workflowMapping.workflowUuid() : workflow.getLabel();
    }

    private List<AutomationWorkflowProjectConfigurationVersionImpactDTO.Workflow> getMoveImpactWorkflows(
        List<WorkflowMapping> movingWorkflowMappings, IntegrationInstanceConfiguration targetConfiguration) {

        Integration integration = integrationService.getIntegration(targetConfiguration.getIntegrationId());

        Map<String, List<WorkflowMapping>> workflowMappingsByUuid = movingWorkflowMappings.stream()
            .filter(workflowMapping -> workflowMapping.integrationInstanceWorkflow()
                .isEnabled())
            .filter(workflowMapping -> workflowMapping.targetWorkflow() != null)
            .collect(Collectors.groupingBy(WorkflowMapping::workflowUuid));

        List<AutomationWorkflowProjectConfigurationVersionImpactDTO.Workflow> impactedWorkflows = new ArrayList<>();

        for (Map.Entry<String, List<WorkflowMapping>> entry : workflowMappingsByUuid.entrySet()) {
            List<WorkflowMapping> workflowMappings = entry.getValue();

            IntegrationInstanceConfigurationWorkflow targetWorkflow = Objects.requireNonNull(
                workflowMappings.getFirst()
                    .targetWorkflow());

            Workflow workflow = workflowService.getWorkflow(targetWorkflow.getWorkflowId());

            Set<String> providedConnectionNodeNames = targetWorkflow.getConnections()
                .stream()
                .map(IntegrationInstanceConfigurationWorkflowConnection::getWorkflowNodeName)
                .collect(Collectors.toSet());

            List<String> newRequiredConnections = getRequiredComponentConnections(workflow, integration)
                .stream()
                .filter(componentConnection -> !providedConnectionNodeNames.contains(
                    componentConnection.workflowNodeName()))
                .map(ComponentConnection::componentName)
                .distinct()
                .sorted()
                .toList();

            Set<String> newRequiredInputs = new HashSet<>();
            Set<Long> affectedConnectedUserIds = new HashSet<>();

            for (WorkflowMapping workflowMapping : workflowMappings) {
                Set<String> providedInputNames = new HashSet<>(
                    workflowMapping.integrationInstanceWorkflow()
                        .getInputs()
                        .keySet());

                providedInputNames.addAll(targetWorkflow.getInputs()
                    .keySet());

                List<String> missingInputNames = workflow.getInputs()
                    .stream()
                    .filter(Workflow.Input::required)
                    .map(Workflow.Input::name)
                    .filter(inputName -> !providedInputNames.contains(inputName))
                    .toList();

                newRequiredInputs.addAll(missingInputNames);

                if (!missingInputNames.isEmpty() || !newRequiredConnections.isEmpty()) {
                    affectedConnectedUserIds.add(workflowMapping.connectedUserId());
                }
            }

            if (!affectedConnectedUserIds.isEmpty()) {
                impactedWorkflows.add(
                    new AutomationWorkflowProjectConfigurationVersionImpactDTO.Workflow(
                        entry.getKey(),
                        StringUtils.isBlank(workflow.getLabel()) ? entry.getKey() : workflow.getLabel(),
                        newRequiredInputs.stream()
                            .sorted()
                            .toList(),
                        newRequiredConnections, affectedConnectedUserIds.size()));
            }
        }

        impactedWorkflows.sort(
            Comparator.comparing(AutomationWorkflowProjectConfigurationVersionImpactDTO.Workflow::label));

        return impactedWorkflows;
    }

    private InstanceEvaluation evaluateIntegrationInstance(
        IntegrationInstance integrationInstance, MoveContext moveContext) {

        long connectedUserId = integrationInstance.getConnectedUserId();

        if (!Objects.equals(
            integrationInstance.getIntegrationInstanceConfigurationId(), moveContext.sourceConfiguration()
                .getId())) {

            return new InstanceEvaluation(
                new ConfigurationMoveResultDTO.Skipped(
                    connectedUserId, ConfigurationMoveResultDTO.Skipped.NOT_ON_SOURCE, List.of()),
                List.of());
        }

        List<WorkflowMapping> workflowMappings = getWorkflowMappings(integrationInstance, moveContext);

        return new InstanceEvaluation(getSkipped(connectedUserId, workflowMappings, moveContext), workflowMappings);
    }

    private InstanceMoveOutcome moveIntegrationInstance(long integrationInstanceId, MoveContext moveContext) {
        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
            integrationInstanceId);

        long connectedUserId = integrationInstance.getConnectedUserId();

        IntegrationInstanceConfiguration sourceConfiguration = moveContext.sourceConfiguration();
        IntegrationInstanceConfiguration targetConfiguration = moveContext.targetConfiguration();

        InstanceEvaluation instanceEvaluation = evaluateIntegrationInstance(integrationInstance, moveContext);

        if (instanceEvaluation.skipped() != null) {
            return new InstanceMoveOutcome(instanceEvaluation.skipped(), false);
        }

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(connectedUserId);

        boolean triggersActive = integrationInstance.isEnabled() && connectedUser.isEnabled() &&
            sourceConfiguration.isEnabled();

        ConfigurationMoveResultDTO.Skipped remapSkipped;

        try {
            if (triggersActive) {
                integrationInstanceFacade.enableIntegrationInstanceWorkflowTriggers(integrationInstanceId, false);
            }

            remapSkipped = requiresNewTransactionTemplate.execute(status -> {
                IntegrationInstance lockedIntegrationInstance = integrationInstanceService.lockIntegrationInstance(
                    integrationInstanceId);

                InstanceEvaluation lockedInstanceEvaluation = evaluateIntegrationInstance(
                    lockedIntegrationInstance, moveContext);

                if (lockedInstanceEvaluation.skipped() != null) {
                    return lockedInstanceEvaluation.skipped();
                }

                remapIntegrationInstance(
                    integrationInstanceId, targetConfiguration.getId(), lockedInstanceEvaluation.workflowMappings());

                return null;
            });
        } catch (RuntimeException exception) {
            if (triggersActive) {
                restoreTriggers(integrationInstanceId);
            }

            throw exception;
        }

        if (remapSkipped != null) {
            if (triggersActive) {
                restoreTriggers(integrationInstanceId);
            }

            return new InstanceMoveOutcome(remapSkipped, false);
        }

        boolean needsAttention = false;

        if (integrationInstance.isEnabled() && connectedUser.isEnabled() && targetConfiguration.isEnabled()) {
            try {
                integrationInstanceFacade.enableIntegrationInstanceWorkflowTriggers(integrationInstanceId, true);
            } catch (RuntimeException exception) {
                log.warn(
                    "Enabling the triggers of moved integration instance id={} failed", integrationInstanceId,
                    exception);

                needsAttention = true;
            }
        }

        return new InstanceMoveOutcome(null, needsAttention);
    }

    private void remapIntegrationInstance(
        long integrationInstanceId, long targetConfigurationId, List<WorkflowMapping> workflowMappings) {

        for (WorkflowMapping workflowMapping : workflowMappings) {
            IntegrationInstanceWorkflow integrationInstanceWorkflow = workflowMapping.integrationInstanceWorkflow();
            IntegrationInstanceConfigurationWorkflow targetWorkflow = workflowMapping.targetWorkflow();

            if (targetWorkflow == null) {
                integrationInstanceWorkflowService.delete(integrationInstanceWorkflow.getId());
            } else {
                integrationInstanceWorkflowService.updateIntegrationInstanceConfigurationWorkflowId(
                    integrationInstanceWorkflow.getId(), targetWorkflow.getId());
            }
        }

        integrationInstanceService.updateIntegrationInstanceConfigurationId(
            integrationInstanceId, targetConfigurationId);
    }

    private void restoreTriggers(long integrationInstanceId) {
        try {
            integrationInstanceFacade.enableIntegrationInstanceWorkflowTriggers(integrationInstanceId, true);
        } catch (RuntimeException exception) {
            log.warn(
                "Restoring the triggers of integration instance id={} after a failed move failed",
                integrationInstanceId, exception);
        }
    }

    private static List<Long> getSortedDistinct(List<Long> connectedUserIds) {
        return connectedUserIds.stream()
            .distinct()
            .sorted()
            .toList();
    }

    private static void validateSameIntegrationAndEnvironment(
        IntegrationInstanceConfiguration sourceConfiguration, IntegrationInstanceConfiguration targetConfiguration) {

        if (!Objects.equals(sourceConfiguration.getIntegrationId(), targetConfiguration.getIntegrationId()) ||
            sourceConfiguration.getEnvironment() != targetConfiguration.getEnvironment()) {

            throw new ConfigurationException(
                "Integration instance configuration id=%s does not belong to integration id=%s in environment=%s"
                    .formatted(
                        targetConfiguration.getId(), sourceConfiguration.getIntegrationId(),
                        sourceConfiguration.getEnvironment()),
                IntegrationInstanceConfigurationErrorType.INSTANCE_CONFIGURATION_MISMATCH);
        }
    }

    private List<ComponentConnection> getRequiredComponentConnections(Workflow workflow, Integration integration) {
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

        return requiredComponentConnections.stream()
            .filter(workflowConnection -> !Objects.equals(
                workflowConnection.componentName(), integration.getComponentName()))
            .toList();
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

    private record InstanceMoveOutcome(ConfigurationMoveResultDTO.@Nullable Skipped skipped, boolean needsAttention) {

        private static InstanceMoveOutcome skipped(long connectedUserId, String reason, List<String> workflowLabels) {
            return new InstanceMoveOutcome(
                new ConfigurationMoveResultDTO.Skipped(connectedUserId, reason, workflowLabels), false);
        }
    }

    private record InstanceEvaluation(
        ConfigurationMoveResultDTO.@Nullable Skipped skipped, List<WorkflowMapping> workflowMappings) {
    }

    private record MoveEvaluation(
        List<Long> movingConnectedUserIds, List<WorkflowMapping> movingWorkflowMappings,
        List<ConfigurationMoveResultDTO.Skipped> skipped) {
    }

    private record MoveContext(
        IntegrationInstanceConfiguration sourceConfiguration, IntegrationInstanceConfiguration targetConfiguration,
        Map<String, IntegrationInstanceConfigurationWorkflow> targetWorkflowsByUuid,
        Map<String, String> workflowUuidsByWorkflowId) {
    }

    private record UpgradeSource(
        MoveContext moveContext, Map<Long, List<IntegrationInstance>> instancesByConnectedUserId,
        List<Long> connectedUserIds) {
    }

    private record WorkflowMapping(
        long connectedUserId, IntegrationInstanceWorkflow integrationInstanceWorkflow,
        IntegrationInstanceConfigurationWorkflow sourceWorkflow,
        @Nullable IntegrationInstanceConfigurationWorkflow targetWorkflow, String workflowUuid) {

        private boolean hasEnabledTargetWorkflow() {
            IntegrationInstanceConfigurationWorkflow enabledTargetWorkflow = targetWorkflow;

            return enabledTargetWorkflow != null && enabledTargetWorkflow.isEnabled();
        }
    }
}
