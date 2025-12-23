/*
 * Copyright 2025 ByteChef
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

package com.bytechef.automation.workflow.coordinator.task.dispatcher;

import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherPreSendProcessor;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.workflow.coordinator.AbstractDispatcherPreSendProcessor;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@Order(2)
public class ProjectTaskDispatcherPreSendProcessor extends AbstractDispatcherPreSendProcessor
    implements TaskDispatcherPreSendProcessor {

    private final JobService jobService;
    private final PrincipalJobService principalJobService;
    private final JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;

    @SuppressFBWarnings("EI")
    public ProjectTaskDispatcherPreSendProcessor(
        JobService jobService, ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
        PrincipalJobService principalJobService, JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry) {

        super(projectDeploymentWorkflowService);

        this.jobService = jobService;
        this.principalJobService = principalJobService;
        this.jobPrincipalAccessorRegistry = jobPrincipalAccessorRegistry;
    }

    @Override
    public TaskExecution process(TaskExecution taskExecution) {
        Job job = jobService.getJob(Validate.notNull(taskExecution.getJobId(), "jobId"));

        Long projectDeploymentId = principalJobService.getJobPrincipalId(
            Validate.notNull(job.getId(), "id"), ModeType.AUTOMATION);

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

        taskExecution.putMetadata(MetadataConstants.TYPE, ModeType.AUTOMATION);
        taskExecution.putMetadata(MetadataConstants.WORKFLOW_ID, job.getWorkflowId());

        // Derive and pass environment id for downstream components
        int environmentId = (int) jobPrincipalAccessorRegistry
            .getJobPrincipalAccessor(ModeType.AUTOMATION)
            .getEnvironmentId(projectDeploymentId);
        taskExecution.putMetadata(MetadataConstants.ENVIRONMENT_ID, environmentId);

        return taskExecution;
    }

    @Override
    public boolean canProcess(TaskExecution taskExecution) {
        Job job = jobService.getJob(Validate.notNull(taskExecution.getJobId(), "jobId"));

        Long projectDeploymentId = OptionalUtils.orElse(
            principalJobService.fetchJobPrincipalId(Validate.notNull(job.getId(), "id"), ModeType.AUTOMATION),
            null);

        return projectDeploymentId != null;
    }
}
