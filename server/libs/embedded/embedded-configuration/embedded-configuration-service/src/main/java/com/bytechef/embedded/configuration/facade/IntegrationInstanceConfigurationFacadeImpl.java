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
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.embedded.configuration.domain.Integration;
import com.bytechef.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflowConnection;
import com.bytechef.embedded.configuration.dto.IntegrationInstanceConfigurationDTO;
import com.bytechef.embedded.configuration.dto.IntegrationInstanceConfigurationWorkflowDTO;
import com.bytechef.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.embedded.configuration.service.IntegrationService;
import com.bytechef.platform.component.registry.domain.TriggerDefinition;
import com.bytechef.platform.component.registry.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.exception.ApplicationException;
import com.bytechef.platform.configuration.facade.WorkflowConnectionFacade;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.constant.Type;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.workflow.execution.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.facade.InstanceJobFacade;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.service.InstanceJobService;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class IntegrationInstanceConfigurationFacadeImpl implements IntegrationInstanceConfigurationFacade {

    private final InstanceJobFacade instanceJobFacade;
    private final InstanceJobService instanceJobService;
    private final JobService jobService;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final IntegrationService integrationService;
    private final TagService tagService;
    private final TriggerDefinitionService triggerDefinitionService;
    private final TriggerLifecycleFacade triggerLifecycleFacade;
    private final String webhookUrl;
    private final WorkflowConnectionFacade workflowConnectionFacade;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public IntegrationInstanceConfigurationFacadeImpl(
        InstanceJobFacade instanceJobFacade, InstanceJobService instanceJobService, JobService jobService,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationService integrationService, TagService tagService, TriggerDefinitionService triggerDefinitionService,
        TriggerLifecycleFacade triggerLifecycleFacade, @Value("${bytechef.webhook-url}") String webhookUrl,
        WorkflowConnectionFacade workflowConnectionFacade, WorkflowService workflowService) {

        this.instanceJobFacade = instanceJobFacade;
        this.instanceJobService = instanceJobService;
        this.jobService = jobService;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.integrationService = integrationService;
        this.tagService = tagService;
        this.triggerDefinitionService = triggerDefinitionService;
        this.triggerLifecycleFacade = triggerLifecycleFacade;
        this.webhookUrl = webhookUrl;
        this.workflowConnectionFacade = workflowConnectionFacade;
        this.workflowService = workflowService;
    }

    @Override
    public IntegrationInstanceConfigurationDTO createIntegrationInstanceConfiguration(
        IntegrationInstanceConfigurationDTO integrationInstanceConfigurationDTO) {

        IntegrationInstanceConfiguration integrationInstanceConfiguration = integrationInstanceConfigurationDTO
            .toIntegrationInstanceConfiguration();

        long integrationId = Validate.notNull(integrationInstanceConfiguration.getIntegrationId(), "integrationId");

        if (!integrationService.isIntegrationEnabled(integrationId)) {
            throw new ApplicationException(
                "Integration id=%s is not published".formatted(
                    integrationId),
                IntegrationInstanceConfiguration.class, 100);
        }

        List<Tag> tags = checkTags(integrationInstanceConfigurationDTO.tags());

        if (!tags.isEmpty()) {
            integrationInstanceConfiguration.setTags(tags);
        }

        integrationInstanceConfiguration =
            integrationInstanceConfigurationService.create(integrationInstanceConfiguration);

        List<IntegrationInstanceConfigurationWorkflow> integrationInstanceConfigurationWorkflows =
            createIntegrationInstanceConfigurationWorkflows(
                integrationInstanceConfiguration, CollectionUtils.map(
                    integrationInstanceConfigurationDTO.integrationInstanceConfigurationWorkflows(),
                    IntegrationInstanceConfigurationWorkflowDTO::toIntegrationInstanceConfigurationWorkflow));

        return new IntegrationInstanceConfigurationDTO(
            integrationInstanceConfiguration, CollectionUtils.map(
                integrationInstanceConfigurationWorkflows,
                integrationInstanceConfigurationWorkflow -> new IntegrationInstanceConfigurationWorkflowDTO(
                    integrationInstanceConfigurationWorkflow,
                    getWorkflowLastExecutionDate(integrationInstanceConfigurationWorkflow.getWorkflowId()),
                    getStaticWebhookUrl(
                        integrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationId(),
                        integrationInstanceConfigurationWorkflow.getWorkflowId()))),
            integrationService.getIntegration(integrationInstanceConfiguration.getIntegrationId()),
            getIntegrationInstanceConfigurationLastExecutionDate(
                Validate.notNull(integrationInstanceConfiguration.getId(), "id")),
            tags);
    }

    private List<IntegrationInstanceConfigurationWorkflow> createIntegrationInstanceConfigurationWorkflows(
        IntegrationInstanceConfiguration integrationInstanceConfiguration,
        List<IntegrationInstanceConfigurationWorkflow> integrationInstanceConfigurationWorkflows) {

        integrationInstanceConfigurationWorkflows = integrationInstanceConfigurationWorkflowService.create(
            integrationInstanceConfigurationWorkflows
                .stream()
                .peek(integrationInstanceConfigurationWorkflow -> {
                    if (integrationInstanceConfigurationWorkflow.isEnabled()) {
                        validateInputs(integrationInstanceConfigurationWorkflow);
                    }

                    integrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationId(
                        Validate.notNull(integrationInstanceConfiguration.getId(), "id"));
                })
                .toList());

        return integrationInstanceConfigurationWorkflows;
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public long createIntegrationInstanceConfigurationWorkflowJob(Long id, String workflowId) {
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(id, workflowId);

        return instanceJobFacade.createJob(
            new JobParameters(workflowId, integrationInstanceConfigurationWorkflow.getInputs()), id, Type.AUTOMATION);
    }

    @Override
    public void deleteIntegrationInstanceConfiguration(long id) {
        List<IntegrationInstanceConfigurationWorkflow> integrationInstanceConfigurationWorkflows =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(id);

        if (OptionalUtils.isPresent(instanceJobService.fetchLastJobId(id, Type.AUTOMATION))) {
            throw new ApplicationException(
                "IntegrationInstanceConfiguration id=%s has executed workflows".formatted(id),
                IntegrationInstanceConfiguration.class, 101);
        }

        for (IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow : integrationInstanceConfigurationWorkflows) {

            if (integrationInstanceConfigurationWorkflow.isEnabled()) {
                throw new ApplicationException(
                    "IntegrationInstanceConfigurationWorkflow id=%s must be disabled".formatted(
                        integrationInstanceConfigurationWorkflow.getId()),
                    IntegrationInstanceConfiguration.class, 102);
            }

            integrationInstanceConfigurationWorkflowService.delete(integrationInstanceConfigurationWorkflow.getId());
        }

        integrationInstanceConfigurationService.delete(id);

// TODO find a way to delete ll tags not referenced anymore
//        integration.getTagIds()
//            .forEach(tagService::delete);
    }

    @Override
    public void enableIntegrationInstanceConfiguration(long integrationInstanceConfigurationId, boolean enable) {
        List<IntegrationInstanceConfigurationWorkflow> integrationInstanceConfigurationWorkflows =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(
                integrationInstanceConfigurationId);

        for (IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow : integrationInstanceConfigurationWorkflows) {

            if (!integrationInstanceConfigurationWorkflow.isEnabled()) {
                continue;
            }

            if (enable) {
                enableWorkflowTriggers(integrationInstanceConfigurationWorkflow);
            } else {
                disableWorkflowTriggers(integrationInstanceConfigurationWorkflow);
            }
        }

        integrationInstanceConfigurationService.updateEnabled(integrationInstanceConfigurationId, enable);
    }

    @Override
    public void enableIntegrationInstanceConfigurationWorkflow(
        long integrationInstanceConfigurationId, String workflowId, boolean enable) {

        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                integrationInstanceConfigurationId, workflowId);

        IntegrationInstanceConfiguration integrationInstanceConfiguration =
            integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
                integrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationId());

        if (integrationInstanceConfiguration.isEnabled()) {
            if (enable) {
                enableWorkflowTriggers(integrationInstanceConfigurationWorkflow);
            } else {
                disableWorkflowTriggers(integrationInstanceConfigurationWorkflow);
            }
        }

        integrationInstanceConfigurationWorkflowService.updateEnabled(
            integrationInstanceConfigurationWorkflow.getId(), enable);
    }

    @Override
    @Transactional(readOnly = true)
    public IntegrationInstanceConfigurationDTO getIntegrationInstanceConfiguration(long id) {
        IntegrationInstanceConfiguration integrationInstanceConfiguration = integrationInstanceConfigurationService
            .getIntegrationInstanceConfiguration(id);

        return new IntegrationInstanceConfigurationDTO(
            integrationInstanceConfiguration,
            CollectionUtils.map(
                integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(id),
                integrationInstanceConfigurationWorkflow -> new IntegrationInstanceConfigurationWorkflowDTO(
                    integrationInstanceConfigurationWorkflow,
                    getWorkflowLastExecutionDate(integrationInstanceConfigurationWorkflow.getWorkflowId()),
                    getStaticWebhookUrl(
                        integrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationId(),
                        integrationInstanceConfigurationWorkflow.getWorkflowId()))),
            integrationService.getIntegration(integrationInstanceConfiguration.getIntegrationId()),
            getIntegrationInstanceConfigurationLastExecutionDate(
                Validate.notNull(integrationInstanceConfiguration.getId(), "id")),
            tagService.getTags(integrationInstanceConfiguration.getTagIds()));
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
            integrationInstanceConfiguration -> new IntegrationInstanceConfigurationDTO(
                integrationInstanceConfiguration,
                CollectionUtils.map(
                    CollectionUtils.filter(
                        integrationInstanceConfigurationWorkflows,
                        integrationInstanceConfigurationWorkflow -> Objects.equals(
                            integrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationId(),
                            integrationInstanceConfiguration.getId())),
                    integrationInstanceConfigurationWorkflow -> new IntegrationInstanceConfigurationWorkflowDTO(
                        integrationInstanceConfigurationWorkflow,
                        getWorkflowLastExecutionDate(integrationInstanceConfigurationWorkflow.getWorkflowId()),
                        getStaticWebhookUrl(
                            integrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationId(),
                            integrationInstanceConfigurationWorkflow.getWorkflowId()))),
                CollectionUtils.getFirst(
                    integrations, integration -> Objects.equals(
                        integration.getId(), integrationInstanceConfiguration.getIntegrationId())),
                getIntegrationInstanceConfigurationLastExecutionDate(
                    Validate.notNull(integrationInstanceConfiguration.getId(), "id")),
                filterTags(tags, integrationInstanceConfiguration)));
    }

    @Override
    public IntegrationInstanceConfigurationDTO updateIntegrationInstanceConfiguration(
        IntegrationInstanceConfigurationDTO integrationInstanceConfigurationDTO) {

        List<IntegrationInstanceConfigurationWorkflow> integrationInstanceConfigurationWorkflows =
            integrationInstanceConfigurationDTO.integrationInstanceConfigurationWorkflows()
                .stream()
                .map(IntegrationInstanceConfigurationWorkflowDTO::toIntegrationInstanceConfigurationWorkflow)
                .peek(this::validateInputs)
                .toList();

        integrationInstanceConfigurationWorkflows = integrationInstanceConfigurationWorkflowService.update(
            integrationInstanceConfigurationWorkflows);

        List<Tag> tags = checkTags(integrationInstanceConfigurationDTO.tags());

        return new IntegrationInstanceConfigurationDTO(
            integrationInstanceConfigurationService.update(
                integrationInstanceConfigurationDTO.toIntegrationInstanceConfiguration()),
            CollectionUtils.map(
                integrationInstanceConfigurationWorkflows,
                integrationInstanceConfigurationWorkflow -> new IntegrationInstanceConfigurationWorkflowDTO(
                    integrationInstanceConfigurationWorkflow,
                    getWorkflowLastExecutionDate(integrationInstanceConfigurationWorkflow.getWorkflowId()),
                    getStaticWebhookUrl(
                        integrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationId(),
                        integrationInstanceConfigurationWorkflow.getWorkflowId()))),
            integrationService.getIntegration(integrationInstanceConfigurationDTO.integrationId()),
            getIntegrationInstanceConfigurationLastExecutionDate(integrationInstanceConfigurationDTO.id()), tags);
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

    private void disableWorkflowTriggers(
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow) {

        Workflow workflow = workflowService.getWorkflow(integrationInstanceConfigurationWorkflow.getWorkflowId());

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

        for (WorkflowTrigger workflowTrigger : workflowTriggers) {
            WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
                Type.AUTOMATION, integrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationId(),
                workflow.getId(), workflowTrigger.getName());

            triggerLifecycleFacade.executeTriggerDisable(
                workflow.getId(), workflowExecutionId, WorkflowNodeType.ofType(workflowTrigger.getType()),
                workflowTrigger.getParameters(),
                getConnectionId(
                    integrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationId(), workflow.getId(),
                    workflowTrigger));
        }
    }

    private void enableWorkflowTriggers(
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow) {

        Workflow workflow = workflowService.getWorkflow(integrationInstanceConfigurationWorkflow.getWorkflowId());

        validateInputs(integrationInstanceConfigurationWorkflow.getInputs(), workflow);

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

        for (WorkflowTrigger workflowTrigger : workflowTriggers) {
            WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
                Type.AUTOMATION, integrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationId(),
                workflow.getId(), workflowTrigger.getName());

            triggerLifecycleFacade.executeTriggerEnable(
                workflow.getId(), workflowExecutionId, WorkflowNodeType.ofType(workflowTrigger.getType()),
                workflowTrigger.getParameters(),
                getConnectionId(
                    integrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationId(), workflow.getId(),
                    workflowTrigger),
                getWebhookUrl(workflowExecutionId));
        }
    }

    private List<Tag> filterTags(List<Tag> tags, IntegrationInstanceConfiguration integrationInstanceConfiguration) {
        return CollectionUtils.filter(tags, tag -> containsTag(integrationInstanceConfiguration, tag));
    }

    private Long getConnectionId(
        long integrationInstanceConfigurationId, String workflowId, WorkflowTrigger workflowTrigger) {

        return workflowConnectionFacade
            .getWorkflowConnections(workflowTrigger)
            .stream()
            .findFirst()
            .map(workflowConnection -> getConnectionId(
                integrationInstanceConfigurationId, workflowId, workflowConnection.workflowNodeName(),
                workflowConnection.key()))
            .orElse(null);
    }

    private Long getConnectionId(
        long integrationInstanceConfigurationId, String workflowId, String workflowNodeName,
        String workflowConnectionKey) {

        return integrationInstanceConfigurationWorkflowService
            .fetchIntegrationInstanceConfigurationWorkflowConnection(
                integrationInstanceConfigurationId, workflowId, workflowNodeName, workflowConnectionKey)
            .map(IntegrationInstanceConfigurationWorkflowConnection::getConnectionId)
            .orElse(null);
    }

    private LocalDateTime getJobEndDate(Long jobId) {
        Job job = jobService.getJob(jobId);

        return job.getEndDate();
    }

    private LocalDateTime getIntegrationInstanceConfigurationLastExecutionDate(
        long integrationInstanceConfigurationId) {

        return OptionalUtils.mapOrElse(
            instanceJobService.fetchLastJobId(
                integrationInstanceConfigurationId, Type.AUTOMATION),
            this::getJobEndDate, null);
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

    private String getStaticWebhookUrl(long integrationInstanceConfigurationId, String workflowId) {
        Workflow workflow = workflowService.getWorkflow(workflowId);

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

        for (WorkflowTrigger workflowTrigger : workflowTriggers) {
            WorkflowNodeType triggerWorkflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

            TriggerDefinition triggerDefinition = triggerDefinitionService.getTriggerDefinition(
                triggerWorkflowNodeType.componentName(), triggerWorkflowNodeType.componentVersion(),
                triggerWorkflowNodeType.componentOperationName());

            if (triggerDefinition.getType() == TriggerType.STATIC_WEBHOOK) {
                return getWebhookUrl(
                    WorkflowExecutionId.of(
                        Type.AUTOMATION, integrationInstanceConfigurationId, workflow.getId(),
                        workflowTrigger.getName()));
            }
        }

        return null;
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

    private String getWebhookUrl(WorkflowExecutionId workflowExecutionId) {
        return webhookUrl.replace("{id}", workflowExecutionId.toString());
    }

    private LocalDateTime getWorkflowLastExecutionDate(String workflowId) {
        return OptionalUtils.mapOrElse(
            jobService.fetchLastWorkflowJob(workflowId),
            Job::getEndDate,
            null);
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
