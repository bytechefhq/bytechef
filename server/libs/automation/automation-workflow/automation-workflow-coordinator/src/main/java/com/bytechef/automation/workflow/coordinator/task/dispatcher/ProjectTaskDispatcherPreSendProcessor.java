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

package com.bytechef.automation.workflow.coordinator.task.dispatcher;

import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherPreSendProcessor;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.domain.ProjectInstanceWorkflow;
import com.bytechef.automation.configuration.service.ProjectInstanceWorkflowService;
import com.bytechef.automation.workflow.coordinator.AbstractDispatcherPreSendProcessor;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.constant.AppType;
import com.bytechef.platform.workflow.execution.service.InstanceJobService;
import com.fasterxml.jackson.core.type.TypeReference;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class ProjectTaskDispatcherPreSendProcessor extends AbstractDispatcherPreSendProcessor
    implements TaskDispatcherPreSendProcessor {

    private final JobService jobService;
    private final InstanceJobService instanceJobService;

    @SuppressFBWarnings("EI")
    public ProjectTaskDispatcherPreSendProcessor(
        JobService jobService, ProjectInstanceWorkflowService projectInstanceWorkflowService,
        InstanceJobService instanceJobService) {

        super(projectInstanceWorkflowService);

        this.jobService = jobService;
        this.instanceJobService = instanceJobService;
    }

    @Override
    public TaskExecution process(TaskExecution taskExecution) {
        Job job = jobService.getJob(Validate.notNull(taskExecution.getJobId(), "jobId"));

        Long projectInstanceId = OptionalUtils.orElse(
            instanceJobService.fetchJobInstanceId(Validate.notNull(job.getId(), "id"), AppType.AUTOMATION),
            null);

        if (projectInstanceId != null) {
            taskExecution.putMetadata(MetadataConstants.INSTANCE_ID, projectInstanceId);
        }

        Map<String, Long> connectionIdMap = Map.of();
        Map<String, Map<String, Long>> connectionIdsMap = MapUtils.getMap(
            job.getMetadata(), MetadataConstants.CONNECTION_IDS, new TypeReference<>() {}, Map.of());

        if (connectionIdsMap.containsKey(taskExecution.getName())) {

            // directly coming from .../jobs POST endpoint

            connectionIdMap = connectionIdsMap.get(taskExecution.getName());
        } else if (projectInstanceId != null) {
            // check if stored in workflow connections or defined in the workflow definition

            connectionIdMap = getConnectionIdMap(projectInstanceId, job.getWorkflowId(), taskExecution.getName());
        }

        if (!connectionIdMap.isEmpty()) {
            taskExecution.putMetadata(MetadataConstants.CONNECTION_IDS, connectionIdMap);
        }

        taskExecution.putMetadata(MetadataConstants.TYPE, AppType.AUTOMATION);
        taskExecution.putMetadata(MetadataConstants.WORKFLOW_ID, job.getWorkflowId());

        if (projectInstanceId != null) {
            ProjectInstanceWorkflow projectInstanceWorkflow = projectInstanceWorkflowService.getProjectInstanceWorkflow(
                projectInstanceId, job.getWorkflowId());

            taskExecution.putMetadata(MetadataConstants.INSTANCE_WORKFLOW_ID, projectInstanceWorkflow.getId());
        }

        return taskExecution;
    }
}
