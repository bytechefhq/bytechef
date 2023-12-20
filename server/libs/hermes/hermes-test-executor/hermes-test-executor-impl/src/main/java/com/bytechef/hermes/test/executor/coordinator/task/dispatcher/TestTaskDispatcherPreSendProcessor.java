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

package com.bytechef.hermes.test.executor.coordinator.task.dispatcher;

import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherPreSendProcessor;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.configuration.constant.MetadataConstants;
import com.bytechef.hermes.configuration.domain.WorkflowConnection;
import com.bytechef.hermes.configuration.facade.WorkflowConnectionFacade;
import com.fasterxml.jackson.core.type.TypeReference;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.Validate;

/**
 * @author Ivica Cardic
 */
public class TestTaskDispatcherPreSendProcessor implements TaskDispatcherPreSendProcessor {

    private final JobService jobService;
    private final WorkflowConnectionFacade workflowConnectionFacade;

    @SuppressFBWarnings
    public TestTaskDispatcherPreSendProcessor(
        JobService jobService, WorkflowConnectionFacade workflowConnectionFacade) {

        this.jobService = jobService;
        this.workflowConnectionFacade = workflowConnectionFacade;
    }

    @Override
    public TaskExecution process(TaskExecution taskExecution) {
        Job job = jobService.getJob(Validate.notNull(taskExecution.getJobId(), "jobId"));

        Map<String, Long> connectionIdMap;
        Map<String, Map<String, Map<String, Long>>> jobTaskConnectionMap = getJobTaskConnectionMap(job);

        if (jobTaskConnectionMap.containsKey(taskExecution.getName())) {

            // directly coming from .../workflow-tests POST endpoint

            connectionIdMap = getConnectionIdMap(jobTaskConnectionMap.get(taskExecution.getName()));
        } else {

            // defined in the workflow definition

            connectionIdMap = getConnectionIdMap(
                workflowConnectionFacade.getWorkflowConnections(taskExecution.getWorkflowTask()));
        }

        if (connectionIdMap != null) {
            taskExecution.putMetadata(MetadataConstants.CONNECTION_IDS, connectionIdMap);
        }

        return taskExecution;
    }

    private static Map<String, Long> getConnectionIdMap(Map<String, Map<String, Long>> taskConnectionMap) {
        return MapUtils.toMap(
            taskConnectionMap, Map.Entry::getKey, entry -> MapUtils.getLong(entry.getValue(), WorkflowConnection.ID));
    }

    private static Map<String, Long> getConnectionIdMap(List<WorkflowConnection> workflowConnections) {
        return MapUtils.toMap(
            workflowConnections, WorkflowConnection::getKey,
            workflowConnection -> OptionalUtils.get(workflowConnection.getId()));
    }

    private static Map<String, Map<String, Map<String, Long>>> getJobTaskConnectionMap(Job job) {
        return MapUtils.getMap(
            job.getMetadata(), MetadataConstants.CONNECTIONS, new TypeReference<>() {}, Map.of());
    }
}
