/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.embedded.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParameters;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.embedded.configuration.domain.Integration;
import com.bytechef.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflowConnection;
import com.bytechef.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.embedded.configuration.dto.IntegrationInstanceConfigurationDTO;
import com.bytechef.embedded.configuration.dto.IntegrationInstanceConfigurationWorkflowDTO;
import com.bytechef.embedded.configuration.exception.IntegrationInstanceConfigurationErrorType;
import com.bytechef.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.embedded.configuration.service.IntegrationService;
import com.bytechef.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.platform.component.domain.ConnectionDefinition;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.configuration.domain.WorkflowConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.facade.WorkflowConnectionFacade;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.exception.PlatformException;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import com.bytechef.platform.registry.domain.BaseProperty;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.platform.workflow.execution.facade.InstanceJobFacade;
import com.bytechef.platform.workflow.execution.service.InstanceJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
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
 * @author Ivica Cardic
 */
@Service
@Transactional
public class IntegrationInstanceConfigurationFacadeImpl implements IntegrationInstanceConfigurationFacade {

    private final ConnectionService connectionService;
    private final ConnectionDefinitionService connectionDefinitionService;
    private final InstanceJobFacade instanceJobFacade;
    private final InstanceJobService instanceJobService;
    private final JobFacade jobFacade;
    private final JobService jobService;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final IntegrationService integrationService;
    private final IntegrationWorkflowService integrationWorkflowService;
    private final OAuth2Service oAuth2Service;
    private final TagService tagService;
    private final TriggerExecutionService triggerExecutionService;
    private final WorkflowConnectionFacade workflowConnectionFacade;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public IntegrationInstanceConfigurationFacadeImpl(
        ConnectionService connectionService, ConnectionDefinitionService connectionDefinitionService,
        InstanceJobFacade instanceJobFacade, InstanceJobService instanceJobService, JobFacade jobFacade,
        JobService jobService, IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationService integrationService, IntegrationWorkflowService integrationWorkflowService,
        OAuth2Service oAuth2Service, TagService tagService, TriggerExecutionService triggerExecutionService,
        WorkflowConnectionFacade workflowConnectionFacade, WorkflowService workflowService) {

        this.connectionService = connectionService;
        this.connectionDefinitionService = connectionDefinitionService;
        this.instanceJobFacade = instanceJobFacade;
        this.instanceJobService = instanceJobService;
        this.jobFacade = jobFacade;
        this.jobService = jobService;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.integrationService = integrationService;
        this.integrationWorkflowService = integrationWorkflowService;
        this.oAuth2Service = oAuth2Service;
        this.tagService = tagService;
        this.triggerExecutionService = triggerExecutionService;
        this.workflowConnectionFacade = workflowConnectionFacade;
        this.workflowService = workflowService;
    }

    @Override
    public IntegrationInstanceConfigurationDTO createIntegrationInstanceConfiguration(
        IntegrationInstanceConfigurationDTO integrationInstanceConfigurationDTO) {

        IntegrationInstanceConfiguration integrationInstanceConfiguration = integrationInstanceConfigurationDTO
            .toIntegrationInstanceConfiguration();

        long integrationId = Validate.notNull(integrationInstanceConfiguration.getIntegrationId(), "integrationId");

        Integration integration = integrationService.getIntegration(integrationId);

        if (!integration.isPublished()) {
            throw new PlatformException(
                "Integration id=%s is not published".formatted(integrationId),
                IntegrationInstanceConfigurationErrorType.CREATE_INTEGRATION_INSTANCE_CONFIGURATION);
        }

        if (integration.getLastIntegrationVersion() == integrationInstanceConfiguration.getIntegrationVersion()) {
            throw new PlatformException(
                "Integration version v=%s cannot be in DRAFT".formatted(
                    integrationInstanceConfiguration.getIntegrationVersion()),
                IntegrationInstanceConfigurationErrorType.CREATE_INTEGRATION_INSTANCE_CONFIGURATION);
        }

        int integrationVersion = integrationInstanceConfiguration.getIntegrationVersion();

        List<IntegrationInstanceConfiguration> integrationInstanceConfigurations =
            integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(
                integrationInstanceConfiguration.getEnvironment(), integrationId, null);

        if (integrationInstanceConfigurations.stream()
            .anyMatch(curIntegrationInstanceConfigurations -> curIntegrationInstanceConfigurations
                .getVersion() == integrationVersion)) {

            throw new PlatformException(
                "Instance Configuration is already set for environment=%s and integrationVersion=%s".formatted(
                    integrationInstanceConfiguration.getEnvironment(), integrationVersion),
                IntegrationInstanceConfigurationErrorType.CREATE_INTEGRATION_INSTANCE_CONFIGURATION);
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

        List<IntegrationInstanceConfigurationWorkflow> integrationInstanceConfigurationWorkflows =
            createIntegrationInstanceConfigurationWorkflows(
                integrationInstanceConfiguration, CollectionUtils.map(
                    integrationInstanceConfigurationDTO.integrationInstanceConfigurationWorkflows(),
                    IntegrationInstanceConfigurationWorkflowDTO::toIntegrationInstanceConfigurationWorkflow));

        List<IntegrationWorkflow> integrationWorkflows = getIntegrationWorkflows(integrationInstanceConfiguration);

        return toIntegrationInstanceConfigurationDTO(
            integrationService.getIntegration(integrationInstanceConfiguration.getIntegrationId()),
            integrationInstanceConfiguration, integrationInstanceConfigurationWorkflows, integrationWorkflows, tags);
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public long createIntegrationInstanceConfigurationWorkflowJob(Long id, String workflowId) {
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(id, workflowId);

        return instanceJobFacade.createJob(
            new JobParameters(workflowId, integrationInstanceConfigurationWorkflow.getInputs()), id, ModeType.EMBEDDED);
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

        List<Long> jobIds = instanceJobService.getJobIds(id, ModeType.EMBEDDED);

        for (long jobId : jobIds) {
            triggerExecutionService.deleteJobTriggerExecution(jobId);

            instanceJobService.deleteInstanceJobs(jobId, ModeType.EMBEDDED);

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
        integrationInstanceConfigurationService.updateEnabled(integrationInstanceConfigurationId, enable);
    }

    @Override
    public void enableIntegrationInstanceConfigurationWorkflow(
        long integrationInstanceConfigurationId, String workflowId, boolean enable) {

        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                integrationInstanceConfigurationId, workflowId);

        if (enable) {
            Workflow workflow = workflowService.getWorkflow(workflowId);

            List<WorkflowConnection> requiredWorkflowConnections = CollectionUtils.concat(
                WorkflowTrigger.of(workflow)
                    .stream()
                    .flatMap(workflowTrigger -> CollectionUtils.stream(
                        workflowConnectionFacade.getWorkflowConnections(workflowTrigger)))
                    .filter(WorkflowConnection::required)
                    .toList(),
                workflow.getAllTasks()
                    .stream()
                    .flatMap(workflowTask -> CollectionUtils.stream(
                        workflowConnectionFacade.getWorkflowConnections(workflowTask)))
                    .filter(WorkflowConnection::required)
                    .toList());

            if ((requiredWorkflowConnections.size() - 1) != integrationInstanceConfigurationWorkflow
                .getConnectionsCount()) {

                throw new PlatformException(
                    "Not all required connections are set for a workflow with id=%s".formatted(workflow.getId()),
                    IntegrationInstanceConfigurationErrorType.REQUIRED_WORKFLOW_CONNECTIONS);
            }
        }

        integrationInstanceConfigurationWorkflowService.updateEnabled(
            integrationInstanceConfigurationWorkflow.getId(), enable);
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressFBWarnings("NP")
    public IntegrationInstanceConfigurationDTO getIntegrationInstanceConfiguration(long id) {
        IntegrationInstanceConfiguration integrationInstanceConfiguration = integrationInstanceConfigurationService
            .getIntegrationInstanceConfiguration(id);

        List<IntegrationWorkflow> integrationWorkflows = getIntegrationWorkflows(integrationInstanceConfiguration);
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
        Environment environment, Long integrationId, Long tagId) {

        List<IntegrationInstanceConfiguration> integrationInstanceConfigurations =
            integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(
                environment, integrationId, tagId);

        List<IntegrationInstanceConfigurationWorkflow> integrationInstanceConfigurationWorkflows =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(
                CollectionUtils.map(integrationInstanceConfigurations, IntegrationInstanceConfiguration::getId));
        List<Integration> integrations = getIntegrations(integrationInstanceConfigurations);
        List<Tag> tags = getTags(integrationInstanceConfigurations);

        return CollectionUtils.map(
            integrationInstanceConfigurations,
            integrationInstanceConfiguration -> {
                List<String> workflowIds = getWorkflowIds(integrationInstanceConfiguration);

                return toIntegrationInstanceConfigurationDTO(
                    CollectionUtils.getFirst(
                        integrations, integration -> Objects.equals(
                            integration.getId(), integrationInstanceConfiguration.getIntegrationId())),
                    integrationInstanceConfiguration,
                    CollectionUtils.filter(
                        integrationInstanceConfigurationWorkflows,
                        integrationInstanceConfigurationWorkflow -> Objects.equals(
                            integrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationId(),
                            integrationInstanceConfiguration.getId()) &&
                            workflowIds.contains(integrationInstanceConfigurationWorkflow.getWorkflowId())),
                    getIntegrationWorkflows(integrationInstanceConfiguration),
                    filterTags(tags, integrationInstanceConfiguration));
            });
    }

    @Override
    public IntegrationInstanceConfigurationDTO updateIntegrationInstanceConfiguration(
        IntegrationInstanceConfigurationDTO integrationInstanceConfigurationDTO) {

        IntegrationInstanceConfiguration integrationInstanceConfiguration = integrationInstanceConfigurationDTO
            .toIntegrationInstanceConfiguration();

        List<Tag> tags = checkTags(integrationInstanceConfigurationDTO.tags());

        if (!tags.isEmpty()) {
            integrationInstanceConfiguration.setTags(tags);
        }

        integrationInstanceConfiguration = integrationInstanceConfigurationService.update(
            integrationInstanceConfiguration);

        integrationInstanceConfigurationWorkflowService.deleteIntegrationInstanceConfigurationWorkflows(
            integrationInstanceConfiguration.getId());

        List<IntegrationInstanceConfigurationWorkflow> integrationInstanceConfigurationWorkflows =
            createIntegrationInstanceConfigurationWorkflows(
                integrationInstanceConfiguration, CollectionUtils.map(
                    integrationInstanceConfigurationDTO.integrationInstanceConfigurationWorkflows(),
                    IntegrationInstanceConfigurationWorkflowDTO::toIntegrationInstanceConfigurationWorkflow));

        List<IntegrationWorkflow> integrationWorkflows = getIntegrationWorkflows(integrationInstanceConfigurationDTO);

        return toIntegrationInstanceConfigurationDTO(
            integrationService.getIntegration(integrationInstanceConfigurationDTO.integrationId()),
            integrationInstanceConfiguration, integrationInstanceConfigurationWorkflows, integrationWorkflows,
            tags);
    }

    @Override
    public void updateIntegrationInstanceConfigurationTags(long id, List<Tag> tags) {
        integrationInstanceConfigurationService.update(id, CollectionUtils.map(checkTags(tags), Tag::getId));
    }

    @Override
    public IntegrationInstanceConfigurationWorkflow updateIntegrationInstanceConfigurationWorkflow(
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow) {

        validateInputs(integrationInstanceConfigurationWorkflow);

        return integrationInstanceConfigurationWorkflowService.update(integrationInstanceConfigurationWorkflow);
    }

    private List<Tag> checkTags(List<Tag> tags) {
        return CollectionUtils.isEmpty(tags) ? Collections.emptyList() : tagService.save(tags);
    }

    private static boolean containsTag(IntegrationInstanceConfiguration integrationInstanceConfiguration, Tag tag) {
        List<Long> tagIds = integrationInstanceConfiguration.getTagIds();

        return tagIds.contains(tag.getId());
    }

    private List<IntegrationInstanceConfigurationWorkflow> createIntegrationInstanceConfigurationWorkflows(
        IntegrationInstanceConfiguration integrationInstanceConfiguration,
        List<IntegrationInstanceConfigurationWorkflow> integrationInstanceConfigurationWorkflows) {

        integrationInstanceConfigurationWorkflows = integrationInstanceConfigurationWorkflowService.create(
            integrationInstanceConfigurationWorkflows
                .stream()
                .peek(integrationInstanceConfigurationWorkflow -> {
                    if (integrationInstanceConfigurationWorkflow.isEnabled()) {
                        List<IntegrationInstanceConfigurationWorkflowConnection> integrationInstanceConfigurationWorkflowConnections =
                            integrationInstanceConfigurationWorkflow.getConnections();
                        Workflow workflow = workflowService.getWorkflow(
                            integrationInstanceConfigurationWorkflow.getWorkflowId());

                        validateConnections(integrationInstanceConfigurationWorkflowConnections, workflow);
                        validateInputs(integrationInstanceConfigurationWorkflow.getInputs(), workflow);
                    }

                    integrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationId(
                        Validate.notNull(integrationInstanceConfiguration.getId(), "id"));
                })
                .toList());

        return integrationInstanceConfigurationWorkflows;
    }

    private List<Tag> filterTags(List<Tag> tags, IntegrationInstanceConfiguration integrationInstanceConfiguration) {
        return CollectionUtils.filter(tags, tag -> containsTag(integrationInstanceConfiguration, tag));
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

    private LocalDateTime getIntegrationInstanceConfigurationLastExecutionDate(
        long integrationInstanceConfigurationId) {

        return OptionalUtils.mapOrElse(
            instanceJobService.fetchLastJobId(integrationInstanceConfigurationId, ModeType.EMBEDDED),
            this::getJobEndDate, null);
    }

    @SuppressFBWarnings("NP")
    private List<IntegrationWorkflow> getIntegrationWorkflows(
        IntegrationInstanceConfiguration integrationInstanceConfiguration) {

        return integrationInstanceConfiguration.getIntegrationVersion() == null
            ? List.of()
            : integrationWorkflowService.getIntegrationWorkflows(
                integrationInstanceConfiguration.getIntegrationId(),
                integrationInstanceConfiguration.getIntegrationVersion());
    }

    private List<IntegrationWorkflow> getIntegrationWorkflows(
        IntegrationInstanceConfigurationDTO integrationInstanceConfigurationDTO) {

        return integrationInstanceConfigurationDTO.integrationVersion() == null
            ? List.of()
            : integrationWorkflowService.getIntegrationWorkflows(
                integrationInstanceConfigurationDTO.integrationId(),
                integrationInstanceConfigurationDTO.integrationVersion());
    }

    private List<Integration> getIntegrations(
        List<IntegrationInstanceConfiguration> integrationInstanceConfigurations) {

        return integrationService.getIntegrations(
            integrationInstanceConfigurations
                .stream()
                .map(IntegrationInstanceConfiguration::getIntegrationId)
                .filter(Objects::nonNull)
                .toList());
    }

    private LocalDateTime getJobEndDate(Long jobId) {
        Job job = jobService.getJob(jobId);

        return job.getEndDate();
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

    @SuppressFBWarnings("NP")
    private List<String> getWorkflowIds(IntegrationInstanceConfiguration integrationInstanceConfiguration) {
        return integrationInstanceConfiguration.getIntegrationVersion() == null
            ? List.of()
            : integrationWorkflowService.getWorkflowIds(
                integrationInstanceConfiguration.getIntegrationId(),
                integrationInstanceConfiguration.getIntegrationVersion());
    }

    private LocalDateTime getWorkflowLastExecutionDate(String workflowId) {
        return OptionalUtils.mapOrElse(
            jobService.fetchLastWorkflowJob(workflowId),
            Job::getEndDate,
            null);
    }

    private String getWorkflowReferenceCode(
        String workflowId, Integer integrationVersion, List<IntegrationWorkflow> integrationWorkflows) {

        if (integrationVersion == null) {
            return null;
        }

        return integrationWorkflows.stream()
            .filter(integrationWorkflow -> Objects.equals(integrationWorkflow.getWorkflowId(), workflowId) &&
                integrationWorkflow.getIntegrationVersion() == integrationVersion)
            .findFirst()
            .map(IntegrationWorkflow::getWorkflowReferenceCode)
            .orElseThrow();
    }

    private IntegrationInstanceConfigurationDTO toIntegrationInstanceConfigurationDTO(
        IntegrationInstanceConfiguration integrationInstanceConfiguration,
        List<IntegrationInstanceConfigurationWorkflow> integrationInstanceWorkflows,
        List<IntegrationWorkflow> integrationWorkflows) {

        return toIntegrationInstanceConfigurationDTO(
            integrationService.getIntegration(integrationInstanceConfiguration.getIntegrationId()),
            integrationInstanceConfiguration, integrationInstanceWorkflows, integrationWorkflows,
            tagService.getTags(integrationInstanceConfiguration.getTagIds()));
    }

    private IntegrationInstanceConfigurationDTO toIntegrationInstanceConfigurationDTO(
        Integration integration, IntegrationInstanceConfiguration integrationInstanceConfiguration,
        List<IntegrationInstanceConfigurationWorkflow> integrationInstanceWorkflows,
        List<IntegrationWorkflow> integrationWorkflows, List<Tag> tags) {

        // TODO define scenarios when there are multiple component versions and workflows

        ConnectionDefinition connectionDefinition = connectionDefinitionService.getConnectionDefinition(
            integration.getComponentName(), null);

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
                integrationInstanceConfigurationWorkflow -> new IntegrationInstanceConfigurationWorkflowDTO(
                    integrationInstanceConfigurationWorkflow,
                    getWorkflowLastExecutionDate(integrationInstanceConfigurationWorkflow.getWorkflowId()),
                    getWorkflowReferenceCode(
                        integrationInstanceConfigurationWorkflow.getWorkflowId(),
                        integrationInstanceConfiguration.getIntegrationVersion(), integrationWorkflows))),
            integration,
            getIntegrationInstanceConfigurationLastExecutionDate(
                Validate.notNull(integrationInstanceConfiguration.getId(), "id")),
            tags);
    }

    private void validateConnections(
        List<IntegrationInstanceConfigurationWorkflowConnection> integrationInstanceConfigurationWorkflowConnections,
        Workflow workflow) {

        for (IntegrationInstanceConfigurationWorkflowConnection integrationInstanceConfigurationWorkflowConnection : integrationInstanceConfigurationWorkflowConnections) {

            Connection connection = connectionService.getConnection(
                integrationInstanceConfigurationWorkflowConnection.getConnectionId());

            WorkflowConnection workflowConnection = workflowConnectionFacade.getWorkflowConnection(
                workflow.getId(), integrationInstanceConfigurationWorkflowConnection.getWorkflowNodeName(),
                integrationInstanceConfigurationWorkflowConnection.getKey());

            if (!Objects.equals(connection.getComponentName(), workflowConnection.componentName())) {
                throw new IllegalArgumentException(
                    "Connection component name does not match workflow connection component name");
            }
        }
    }

    private void validateInputs(IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow) {
        Workflow workflow = workflowService.getWorkflow(integrationInstanceConfigurationWorkflow.getWorkflowId());

        validateInputs(integrationInstanceConfigurationWorkflow.getInputs(), workflow);
    }

    private void validateInputs(Map<String, ?> inputs, Workflow workflow) {
        for (Workflow.Input input : workflow.getInputs()) {
            if (input.required()) {
                Validate.isTrue(inputs.containsKey(input.name()), "Missing required param: " + input.name());
                Validate.notEmpty((String) inputs.get(input.name()), "Missing required param: " + input.name());
            }
        }
    }
}
