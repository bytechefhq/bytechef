
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.helios.coordinator.task.dispatcher;

import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherPreSendProcessor;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.commons.util.MapValueUtils;
import com.bytechef.helios.configuration.constant.ProjectConstants;
import com.bytechef.helios.configuration.service.ProjectInstanceService;
import com.bytechef.helios.configuration.service.ProjectInstanceWorkflowService;
import com.bytechef.helios.coordinator.AbstractDispatcherPreSendProcessor;
import com.bytechef.hermes.configuration.connection.WorkflowConnection;
import com.bytechef.hermes.configuration.constant.MetadataConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Ivica Cardic
 */
@Component
public class ProjectTaskDispatcherPreSendProcessor extends AbstractDispatcherPreSendProcessor
    implements TaskDispatcherPreSendProcessor {

    private final JobService jobService;
    private final ProjectInstanceService projectInstanceService;

    @SuppressFBWarnings("EI")
    public ProjectTaskDispatcherPreSendProcessor(
        JobService jobService, ProjectInstanceService projectInstanceService,
        ProjectInstanceWorkflowService projectInstanceWorkflowService) {

        super(projectInstanceWorkflowService);

        this.jobService = jobService;
        this.projectInstanceService = projectInstanceService;
    }

    @Override
    @SuppressFBWarnings("NP")
    public TaskExecution process(TaskExecution taskExecution) {
        Job job = jobService.getJob(Objects.requireNonNull(taskExecution.getJobId()));

        Map<String, Map<String, WorkflowConnection>> jobTaskConnectionMap = getJobTaskConnectionMap(job);

        Map<String, WorkflowConnection> taskConnectionMap;

        if (jobTaskConnectionMap.containsKey(taskExecution.getName())) {
            // directly coming from /core/jobs POST endpoint
            taskConnectionMap = jobTaskConnectionMap.get(taskExecution.getName());
        } else {
            // defined in the workflow definition
            taskConnectionMap = WorkflowConnection.of(taskExecution.getWorkflowTask());
        }

        taskExecution.putMetadata(MetadataConstants.CONNECTION_IDS, getConnectionIdMap(taskConnectionMap));

        projectInstanceService.fetchJobProjectInstance(Objects.requireNonNull(taskExecution.getJobId()))
            .ifPresent(projectInstance -> taskExecution
                .putMetadata(MetadataConstants.INSTANCE_ID, projectInstance.getId())
                .putMetadata(MetadataConstants.INSTANCE_TYPE, ProjectConstants.PROJECT));

        taskExecution.putMetadata(MetadataConstants.WORKFLOW_ID, job.getWorkflowId());

        return taskExecution;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, WorkflowConnection>> getJobTaskConnectionMap(Job job) {
        Map<String, Map<String, Map<String, Object>>> connectionMap = MapValueUtils.get(
            job.getMetadata(), WorkflowConnection.CONNECTIONS, Map.class, Map.of());

        return connectionMap.entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> WorkflowConnection.toMap(entry.getValue(), entry.getKey())));
    }
}
