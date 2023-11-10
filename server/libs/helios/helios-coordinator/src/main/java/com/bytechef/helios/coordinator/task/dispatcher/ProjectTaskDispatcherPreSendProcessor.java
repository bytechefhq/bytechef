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

package com.bytechef.helios.coordinator.task.dispatcher;

import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherPreSendProcessor;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.helios.configuration.constant.ProjectConstants;
import com.bytechef.helios.configuration.service.ProjectInstanceWorkflowService;
import com.bytechef.helios.coordinator.AbstractDispatcherPreSendProcessor;
import com.bytechef.hermes.configuration.connection.WorkflowConnection;
import com.bytechef.hermes.configuration.constant.MetadataConstants;
import com.bytechef.hermes.execution.service.InstanceJobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.springframework.core.ParameterizedTypeReference;
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

        Map<String, Map<String, Map<String, Long>>> jobTaskConnectionMap = getJobTaskConnectionMap(job);
        Map<String, Long> connectionIdMap;
        Long projectInstanceId = OptionalUtils.orElse(
            instanceJobService.fetchJobInstanceId(Validate.notNull(job.getId(), "id"), ProjectConstants.PROJECT_TYPE),
            null);

        if (jobTaskConnectionMap.containsKey(taskExecution.getName())) {

            // directly coming from .../jobs POST endpoint

            connectionIdMap = getConnectionIdMap(jobTaskConnectionMap.get(taskExecution.getName()));
        } else {

            // defined in the workflow definition

            connectionIdMap = getConnectionIdMap(
                projectInstanceId, job.getWorkflowId(), WorkflowConnection.of(taskExecution.getWorkflowTask()));
        }

        taskExecution.putMetadata(MetadataConstants.CONNECTION_IDS, connectionIdMap);

        if (projectInstanceId != null) {
            taskExecution.putMetadata(MetadataConstants.INSTANCE_ID, projectInstanceId);
        }

        taskExecution.putMetadata(MetadataConstants.TYPE, ProjectConstants.PROJECT_TYPE);
        taskExecution.putMetadata(MetadataConstants.WORKFLOW_ID, job.getWorkflowId());

        return taskExecution;
    }

    private static Map<String, Long> getConnectionIdMap(Map<String, Map<String, Long>> taskConnectionMap) {
        return MapUtils.toMap(
            taskConnectionMap, Map.Entry::getKey, entry -> MapUtils.getLong(entry.getValue(), WorkflowConnection.ID));
    }

    private static Map<String, Map<String, Map<String, Long>>> getJobTaskConnectionMap(Job job) {
        return MapUtils.getMap(
            job.getMetadata(), WorkflowConnection.CONNECTIONS, new ParameterizedTypeReference<>() {}, Map.of());
    }
}
