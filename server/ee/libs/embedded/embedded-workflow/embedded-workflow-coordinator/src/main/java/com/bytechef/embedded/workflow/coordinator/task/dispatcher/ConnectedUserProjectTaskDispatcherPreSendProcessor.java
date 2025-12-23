/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.embedded.workflow.coordinator.task.dispatcher;

import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherPreSendProcessor;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.embedded.workflow.coordinator.AbstractConnectedUserProjectDispatcherPreSendProcessor;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@Order(1)
@ConditionalOnEEVersion
public class ConnectedUserProjectTaskDispatcherPreSendProcessor
    extends AbstractConnectedUserProjectDispatcherPreSendProcessor implements TaskDispatcherPreSendProcessor {

    private final ConnectedUserProjectService connectedUserProjectService;
    private final JobService jobService;
    private final PrincipalJobService principalJobService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;

    @SuppressFBWarnings("EI")
    public ConnectedUserProjectTaskDispatcherPreSendProcessor(
        ConnectedUserProjectService connectedUserProjectService, JobService jobService,
        PrincipalJobService principalJobService, ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry) {

        super(projectDeploymentWorkflowService);

        this.jobService = jobService;
        this.connectedUserProjectService = connectedUserProjectService;
        this.principalJobService = principalJobService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.jobPrincipalAccessorRegistry = jobPrincipalAccessorRegistry;
    }

    @Override
    public TaskExecution process(TaskExecution taskExecution) {
        Job job = jobService.getJob(Validate.notNull(taskExecution.getJobId(), "jobId"));

        Long projectDeploymentId = principalJobService.getJobPrincipalId(
            Validate.notNull(job.getId(), "id"), PlatformType.AUTOMATION);

        taskExecution.putMetadata(MetadataConstants.JOB_PRINCIPAL_ID, projectDeploymentId);

        Map<String, Long> connectionIdMap = getConnectionIdMap(
            projectDeploymentId, job.getWorkflowId(), taskExecution.getName());

        if (!connectionIdMap.isEmpty()) {
            taskExecution.putMetadata(MetadataConstants.CONNECTION_IDS, connectionIdMap);
        }

        ProjectDeploymentWorkflow projectDeploymentWorkflow =
            projectDeploymentWorkflowService.getProjectDeploymentWorkflow(
                projectDeploymentId, job.getWorkflowId());

        taskExecution.putMetadata(MetadataConstants.JOB_PRINCIPAL_WORKFLOW_ID, projectDeploymentWorkflow.getId());

        taskExecution.putMetadata(MetadataConstants.TYPE, PlatformType.AUTOMATION);
        taskExecution.putMetadata(MetadataConstants.WORKFLOW_ID, job.getWorkflowId());

        int environmentId = (int) jobPrincipalAccessorRegistry
            .getJobPrincipalAccessor(PlatformType.AUTOMATION)
            .getEnvironmentId(projectDeploymentId);
        taskExecution.putMetadata(MetadataConstants.ENVIRONMENT_ID, environmentId);

        return taskExecution;
    }

    @Override
    public boolean canProcess(TaskExecution taskExecution) {
        Job job = jobService.getJob(Validate.notNull(taskExecution.getJobId(), "jobId"));

        Long projectDeploymentId = OptionalUtils.orElse(
            principalJobService.fetchJobPrincipalId(Validate.notNull(job.getId(), "id"), PlatformType.AUTOMATION),
            null);

        if (projectDeploymentId == null) {
            return false;
        }

        return connectedUserProjectService.containsProjectDeployment(projectDeploymentId);
    }
}
