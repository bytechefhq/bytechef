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
import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.component.constant.MetadataConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import tools.jackson.core.type.TypeReference;

/**
 * @author Ivica Cardic
 */
public class TestTaskDispatcherPreSendProcessor implements TaskDispatcherPreSendProcessor {

    private final JobService jobService;

    @SuppressFBWarnings
    public TestTaskDispatcherPreSendProcessor(JobService jobService) {
        this.jobService = jobService;
    }

    @Override
    public TaskExecution process(TaskExecution taskExecution) {
        Job job = jobService.getJob(Validate.notNull(taskExecution.getJobId(), "jobId"));

        Map<String, Map<String, Long>> connectionIdsMap = MapUtils.getMap(
            job.getMetadata(), MetadataConstants.CONNECTION_IDS, new TypeReference<>() {}, Map.of());

        if (connectionIdsMap.containsKey(taskExecution.getName())) {
            taskExecution.putMetadata(MetadataConstants.CONNECTION_IDS, connectionIdsMap.get(taskExecution.getName()));
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
}
