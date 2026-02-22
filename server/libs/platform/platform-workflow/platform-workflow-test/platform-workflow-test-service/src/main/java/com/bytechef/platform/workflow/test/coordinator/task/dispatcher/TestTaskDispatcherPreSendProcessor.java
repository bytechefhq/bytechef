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

package com.bytechef.platform.workflow.test.coordinator.task.dispatcher;

import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherPreSendProcessor;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.Validate;

/**
 * @author Ivica Cardic
 */
public class TestTaskDispatcherPreSendProcessor implements TaskDispatcherPreSendProcessor {

    private final JobService jobService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    public TestTaskDispatcherPreSendProcessor(
        JobService jobService, WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.jobService = jobService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @Override
    public TaskExecution process(TaskExecution taskExecution) {
        Job job = jobService.getJob(Validate.notNull(taskExecution.getJobId(), "jobId"));

        Map<String, Long> connectionIdMap = getConnectionIdMap(job.getWorkflowId(), taskExecution.getName());

        if (!connectionIdMap.isEmpty()) {
            taskExecution.putMetadata(MetadataConstants.CONNECTION_IDS, connectionIdMap);
        }

        taskExecution.putMetadata(MetadataConstants.ENVIRONMENT_ID, 0);
        taskExecution.putMetadata(MetadataConstants.EDITOR_ENVIRONMENT, true);
        taskExecution.putMetadata(MetadataConstants.WORKFLOW_ID, job.getWorkflowId());

        return taskExecution;
    }

    @Override
    public boolean canProcess(TaskExecution taskExecution) {
        return true;
    }

    private Map<String, Long> getConnectionIdMap(String workflowId, String workflowNodeName) {
        List<WorkflowTestConfigurationConnection> connections =
            workflowTestConfigurationService.getWorkflowTestConfigurationConnections(workflowId, workflowNodeName, 0);

        Map<String, Long> connectionIdMap = new HashMap<>();

        for (WorkflowTestConfigurationConnection connection : connections) {
            connectionIdMap.put(connection.getWorkflowConnectionKey(), connection.getConnectionId());
        }

        return connectionIdMap;
    }
}
