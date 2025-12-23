/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.embedded.workflow.coordinator.task.dispatcher;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherPreSendProcessor;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.embedded.workflow.coordinator.AbstractDispatcherPreSendProcessor;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@Order(3)
@ConditionalOnEEVersion
public class IntegrationTaskDispatcherPreSendProcessor extends AbstractDispatcherPreSendProcessor
    implements TaskDispatcherPreSendProcessor {

    private final JobService jobService;
    private final PrincipalJobService principalJobService;
    private final IntegrationInstanceService integrationInstanceService;
    private final IntegrationInstanceWorkflowService integrationInstanceWorkflowService;
    private final IntegrationService integrationService;
    private final JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;

    @SuppressFBWarnings("EI")
    public IntegrationTaskDispatcherPreSendProcessor(
        JobService jobService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        PrincipalJobService principalJobService, IntegrationInstanceService integrationInstanceService,
        IntegrationInstanceWorkflowService integrationInstanceWorkflowService, IntegrationService integrationService,
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry) {

        super(integrationInstanceConfigurationWorkflowService);

        this.jobService = jobService;
        this.principalJobService = principalJobService;
        this.integrationInstanceService = integrationInstanceService;
        this.integrationInstanceWorkflowService = integrationInstanceWorkflowService;
        this.integrationService = integrationService;
        this.jobPrincipalAccessorRegistry = jobPrincipalAccessorRegistry;
    }

    @Override
    public TaskExecution process(TaskExecution taskExecution) {
        Job job = jobService.getJob(Validate.notNull(taskExecution.getJobId(), "jobId"));

        Long integrationInstanceId = principalJobService.getJobPrincipalId(
            Validate.notNull(job.getId(), "id"), PlatformType.EMBEDDED);

        taskExecution.putMetadata(MetadataConstants.JOB_PRINCIPAL_ID, integrationInstanceId);

        Map<String, Long> connectionIdMap = getConnectionIdMap(
            integrationInstanceId, job.getWorkflowId(), taskExecution.getName());

        WorkflowTask workflowTask = taskExecution.getWorkflowTask();

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTask.getType());

        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
            integrationInstanceId);

        Integration integration = integrationService.getIntegrationInstanceIntegration(integrationInstanceId);

        if (Objects.equals(integration.getComponentName(), workflowNodeType.name())) {
            connectionIdMap = MapUtils.concat(
                connectionIdMap, Map.of(workflowTask.getName(), integrationInstance.getConnectionId()));
        }

        if (!connectionIdMap.isEmpty()) {
            taskExecution.putMetadata(MetadataConstants.CONNECTION_IDS, connectionIdMap);
        }

        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            integrationInstanceConfigurationWorkflowService
                .getIntegrationInstanceConfigurationWorkflows(
                    integrationInstance.getIntegrationInstanceConfigurationId())
                .stream()
                .filter(curIntegrationInstanceConfigurationWorkflow -> Objects.equals(
                    curIntegrationInstanceConfigurationWorkflow.getWorkflowId(), job.getWorkflowId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("IntegrationInstanceConfigurationWorkflow not found"));

        List<IntegrationInstanceWorkflow> integrationInstanceWorkflows =
            integrationInstanceWorkflowService.getIntegrationInstanceWorkflows(integrationInstance.getId());

        IntegrationInstanceWorkflow integrationInstanceWorkflow = integrationInstanceWorkflows.stream()
            .filter(curIntegrationInstanceWorkflow -> Objects.equals(
                curIntegrationInstanceWorkflow.getIntegrationInstanceConfigurationWorkflowId(),
                integrationInstanceConfigurationWorkflow.getId()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("IntegrationInstanceWorkflow not found"));

        taskExecution.putMetadata(MetadataConstants.JOB_PRINCIPAL_WORKFLOW_ID, integrationInstanceWorkflow.getId());

        taskExecution.putMetadata(MetadataConstants.TYPE, PlatformType.EMBEDDED);
        taskExecution.putMetadata(MetadataConstants.WORKFLOW_ID, job.getWorkflowId());

        int environmentId = (int) jobPrincipalAccessorRegistry
            .getJobPrincipalAccessor(PlatformType.EMBEDDED)
            .getEnvironmentId(integrationInstanceId);
        taskExecution.putMetadata(MetadataConstants.ENVIRONMENT_ID, environmentId);

        return taskExecution;
    }

    @Override
    public boolean canProcess(TaskExecution taskExecution) {
        Job job = jobService.getJob(Validate.notNull(taskExecution.getJobId(), "jobId"));

        Long integrationInstanceId = OptionalUtils.orElse(
            principalJobService.fetchJobPrincipalId(Validate.notNull(job.getId(), "id"), PlatformType.EMBEDDED), null);

        return integrationInstanceId != null;
    }
}
