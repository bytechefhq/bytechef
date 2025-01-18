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
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.embedded.configuration.domain.Integration;
import com.bytechef.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflowConnection;
import com.bytechef.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.embedded.configuration.dto.IntegrationInstanceDTO;
import com.bytechef.embedded.configuration.dto.IntegrationInstanceWorkflowDTO;
import com.bytechef.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.embedded.configuration.service.IntegrationService;
import com.bytechef.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.facade.WorkflowConnectionFacade;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.workflow.execution.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class IntegrationInstanceFacadeImpl implements IntegrationInstanceFacade {

    private final ConnectedUserService connectedUserService;
    private final PrincipalJobService principalJobService;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final IntegrationInstanceWorkflowService integrationInstanceWorkflowService;
    private final IntegrationInstanceService integrationInstanceService;
    private final IntegrationService integrationService;
    private final IntegrationWorkflowService integrationWorkflowService;
    private final JobService jobService;
    private final TriggerLifecycleFacade triggerLifecycleFacade;
    private final WorkflowConnectionFacade workflowConnectionFacade;
    private final WorkflowService workflowService;
    private final String webhookUrl;

    @SuppressFBWarnings("EI")
    public IntegrationInstanceFacadeImpl(
        ConnectedUserService connectedUserService, PrincipalJobService principalJobService,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        ApplicationProperties applicationProperties,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationInstanceWorkflowService integrationInstanceWorkflowService,
        IntegrationInstanceService integrationInstanceService, IntegrationService integrationService,
        IntegrationWorkflowService integrationWorkflowService, JobService jobService,
        TriggerLifecycleFacade triggerLifecycleFacade, WorkflowConnectionFacade workflowConnectionFacade,
        WorkflowService workflowService) {

        this.connectedUserService = connectedUserService;
        this.principalJobService = principalJobService;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.integrationInstanceWorkflowService = integrationInstanceWorkflowService;
        this.integrationInstanceService = integrationInstanceService;
        this.webhookUrl = applicationProperties.getWebhookUrl();
        this.integrationService = integrationService;
        this.integrationWorkflowService = integrationWorkflowService;
        this.jobService = jobService;
        this.triggerLifecycleFacade = triggerLifecycleFacade;
        this.workflowConnectionFacade = workflowConnectionFacade;
        this.workflowService = workflowService;
    }

    @Override
    public void enableIntegrationInstance(long integrationInstanceId, boolean enable) {
        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
            integrationInstanceId);

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(
            integrationInstance.getConnectedUserId());

        if (connectedUser.isEnabled()) {
            enableIntegrationInstanceWorkflowTriggers(integrationInstanceId, enable);
        }

        integrationInstanceService.updateEnabled(integrationInstanceId, enable);
    }

    @Override
    public void enableIntegrationInstanceWorkflow(long integrationInstanceId, String workflowId, boolean enable) {
        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
            integrationInstanceId);

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(
            integrationInstance.getConnectedUserId());

        long integrationInstanceWorkflowId;

        if (connectedUser.isEnabled()) {
            integrationInstanceWorkflowId = enableIntegrationInstanceWorkflowTriggers(
                integrationInstanceId, workflowId, enable);
        } else {
            IntegrationInstanceWorkflow integrationInstanceWorkflow =
                integrationInstanceWorkflowService
                    .fetchIntegrationInstanceWorkflow(integrationInstanceId, workflowId)
                    .orElseGet(() -> {
                        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
                            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                                integrationInstance.getIntegrationInstanceConfigurationId(), workflowId);

                        return integrationInstanceWorkflowService.createIntegrationInstanceWorkflow(
                            integrationInstanceId, integrationInstanceConfigurationWorkflow.getId());
                    });

            integrationInstanceWorkflowId = integrationInstanceWorkflow.getId();
        }

        integrationInstanceWorkflowService.updateEnabled(integrationInstanceWorkflowId, enable);
    }

    @Override
    public void enableIntegrationInstanceWorkflowTriggers(long integrationInstanceId, boolean enable) {
        List<IntegrationInstanceWorkflow> integrationInstanceWorkflows = integrationInstanceWorkflowService
            .getIntegrationInstanceWorkflows(integrationInstanceId);

        for (IntegrationInstanceWorkflow integrationInstanceWorkflow : integrationInstanceWorkflows) {
            IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
                integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                    integrationInstanceWorkflow.getIntegrationInstanceConfigurationWorkflowId());

            if (!integrationInstanceConfigurationWorkflow.isEnabled() || !integrationInstanceWorkflow.isEnabled()) {
                continue;
            }

            if (enable) {
                enableWorkflowTriggers(integrationInstanceWorkflow);
            } else {
                disableWorkflowTriggers(integrationInstanceWorkflow);
            }
        }
    }

    @Override
    public long enableIntegrationInstanceWorkflowTriggers(
        long integrationInstanceId, String workflowId, boolean enable) {

        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
            integrationInstanceId);

        IntegrationInstanceWorkflow integrationInstanceWorkflow =
            integrationInstanceWorkflowService
                .fetchIntegrationInstanceWorkflow(integrationInstanceId, workflowId)
                .orElseGet(() -> {
                    IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
                        integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                            integrationInstance.getIntegrationInstanceConfigurationId(), workflowId);

                    return integrationInstanceWorkflowService.createIntegrationInstanceWorkflow(
                        integrationInstanceId, integrationInstanceConfigurationWorkflow.getId());
                });

        if (integrationInstance.isEnabled()) {
            if (enable) {
                enableWorkflowTriggers(integrationInstanceWorkflow);
            } else {
                disableWorkflowTriggers(integrationInstanceWorkflow);
            }
        }

        return integrationInstanceWorkflow.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public IntegrationInstanceDTO getIntegrationInstance(long id) {
        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(id);

        IntegrationInstanceConfiguration integrationInstanceConfiguration = integrationInstanceConfigurationService
            .getIntegrationInstanceConfiguration(integrationInstance.getIntegrationInstanceConfigurationId());

        List<IntegrationInstanceConfigurationWorkflow> integrationInstanceConfigurationWorkflows =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(
                integrationInstance.getIntegrationInstanceConfigurationId());

        return new IntegrationInstanceDTO(
            integrationInstance, integrationInstanceConfiguration,
            integrationInstanceWorkflowService.getIntegrationInstanceWorkflows(id)
                .stream()
                .map(integrationInstanceWorkflow -> new IntegrationInstanceWorkflowDTO(
                    integrationInstanceWorkflow,
                    CollectionUtils.getFirstFilter(
                        integrationInstanceConfigurationWorkflows,
                        integrationInstanceConfigurationWorkflow -> Objects.equals(
                            integrationInstanceConfigurationWorkflow.getId(),
                            integrationInstanceWorkflow.getIntegrationInstanceConfigurationWorkflowId()),
                        IntegrationInstanceConfigurationWorkflow::getWorkflowId)))
                .collect(Collectors.toSet()),
            getIntegrationInstanceLastExecutionDate(integrationInstance.getId()));
    }

    private void disableWorkflowTriggers(IntegrationInstanceWorkflow integrationInstanceWorkflow) {
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                integrationInstanceWorkflow.getIntegrationInstanceConfigurationWorkflowId());

        IntegrationInstanceConfiguration integrationInstanceConfiguration = integrationInstanceConfigurationService
            .getIntegrationInstanceConfiguration(
                integrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationId());

        IntegrationWorkflow integrationWorkflow = integrationWorkflowService.getWorkflowIntegrationWorkflow(
            integrationInstanceConfigurationWorkflow.getWorkflowId());

        Workflow workflow = workflowService.getWorkflow(integrationWorkflow.getWorkflowId());

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

        for (WorkflowTrigger workflowTrigger : workflowTriggers) {
            WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
                ModeType.EMBEDDED, integrationInstanceWorkflow.getIntegrationInstanceId(),
                integrationWorkflow.getWorkflowReferenceCode(), workflowTrigger.getName());

            triggerLifecycleFacade.executeTriggerDisable(
                workflow.getId(), workflowExecutionId, WorkflowNodeType.ofType(workflowTrigger.getType()),
                workflowTrigger.getParameters(),
                getConnectionId(
                    integrationInstanceConfiguration.getIntegrationId(), integrationInstanceConfiguration.getId(),
                    integrationInstanceWorkflow.getIntegrationInstanceId(), workflow.getId(), workflowTrigger));
        }
    }

    private void enableWorkflowTriggers(IntegrationInstanceWorkflow integrationInstanceWorkflow) {
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                integrationInstanceWorkflow.getIntegrationInstanceConfigurationWorkflowId());

        Workflow workflow = workflowService.getWorkflow(integrationInstanceConfigurationWorkflow.getWorkflowId());

        validateInputs(integrationInstanceWorkflow.getInputs(), workflow);

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);
        IntegrationInstanceConfiguration integrationInstanceConfiguration = integrationInstanceConfigurationService
            .getIntegrationInstanceConfiguration(
                integrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationId());
        IntegrationWorkflow integrationWorkflow = integrationWorkflowService.getWorkflowIntegrationWorkflow(
            workflow.getId());

        for (WorkflowTrigger workflowTrigger : workflowTriggers) {
            WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
                ModeType.EMBEDDED, integrationInstanceWorkflow.getIntegrationInstanceId(),
                integrationWorkflow.getWorkflowReferenceCode(), workflowTrigger.getName());

            triggerLifecycleFacade.executeTriggerEnable(
                workflow.getId(), workflowExecutionId, WorkflowNodeType.ofType(workflowTrigger.getType()),
                workflowTrigger.getParameters(),
                getConnectionId(integrationInstanceConfiguration.getIntegrationId(),
                    integrationInstanceConfiguration.getId(), integrationInstanceWorkflow.getIntegrationInstanceId(),
                    workflow.getId(), workflowTrigger),
                getWebhookUrl(workflowExecutionId));
        }
    }

    private Long getConnectionId(
        long integrationId, long integrationInstanceConfigurationId, long integrationInstanceId, String workflowId,
        WorkflowTrigger workflowTrigger) {

        Integration integration = integrationService.getIntegration(integrationId);

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

        if (Objects.equals(integration.getComponentName(), workflowNodeType.componentName())) {
            IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
                integrationInstanceId);

            return integrationInstance.getConnectionId();
        }

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

    private Instant getIntegrationInstanceLastExecutionDate(long integrationInstanceId) {
        return OptionalUtils.mapOrElse(
            principalJobService.fetchLastJobId(integrationInstanceId, ModeType.EMBEDDED), this::getJobEndDate, null);
    }

    private Instant getJobEndDate(Long jobId) {
        Job job = jobService.getJob(jobId);

        return job.getEndDate();
    }

    private String getWebhookUrl(WorkflowExecutionId workflowExecutionId) {
        return webhookUrl.replace("{id}", workflowExecutionId.toString());
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
