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

package com.bytechef.embedded.workflow.coordinator.task.dispatcher;

import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherPreSendProcessor;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.embedded.workflow.coordinator.AbstractDispatcherPreSendProcessor;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.constant.AppType;
import com.bytechef.platform.workflow.execution.service.InstanceJobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.apache.commons.lang3.Validate;

/**
 * @author Ivica Cardic
 */
// TODD @Component
public class IntegrationTaskDispatcherPreSendProcessor extends AbstractDispatcherPreSendProcessor
    implements TaskDispatcherPreSendProcessor {

    private final JobService jobService;
    private final InstanceJobService instanceJobService;

    @SuppressFBWarnings("EI")
    public IntegrationTaskDispatcherPreSendProcessor(
        JobService jobService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        InstanceJobService instanceJobService) {

        super(integrationInstanceConfigurationWorkflowService);

        this.jobService = jobService;
        this.instanceJobService = instanceJobService;
    }

    @Override
    public TaskExecution process(TaskExecution taskExecution) {
        Job job = jobService.getJob(Validate.notNull(taskExecution.getJobId(), "jobId"));

        Long integrationInstanceId = OptionalUtils.orElse(
            instanceJobService.fetchJobInstanceId(Validate.notNull(job.getId(), "id"), AppType.EMBEDDED), null);

        if (integrationInstanceId != null) {
            taskExecution.putMetadata(MetadataConstants.INSTANCE_ID, integrationInstanceId);
        }

        Map<String, Long> connectionIdMap = getConnectionIdMap(
            integrationInstanceId, job.getWorkflowId(), taskExecution.getName());

        if (!connectionIdMap.isEmpty()) {
            taskExecution.putMetadata(MetadataConstants.CONNECTION_IDS, connectionIdMap);
        }

        taskExecution.putMetadata(MetadataConstants.TYPE, AppType.EMBEDDED);
        taskExecution.putMetadata(MetadataConstants.WORKFLOW_ID, job.getWorkflowId());

        return taskExecution;
    }
}
