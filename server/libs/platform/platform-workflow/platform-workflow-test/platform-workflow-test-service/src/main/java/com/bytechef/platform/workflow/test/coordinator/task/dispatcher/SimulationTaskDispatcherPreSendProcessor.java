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

/**
 * Minimal, fully offline pre-send processor for workflow simulation. It copies only the dry-run related flags from the
 * job metadata onto each task execution so the task/trigger handlers short-circuit to their declared static output
 * instead of making real component calls. Unlike {@link TestTaskDispatcherPreSendProcessor} it performs no connection
 * lookup and no workflow-node-output evaluation.
 *
 * @author Ivica Cardic
 */
public class SimulationTaskDispatcherPreSendProcessor implements TaskDispatcherPreSendProcessor {

    private final JobService jobService;

    @SuppressFBWarnings("EI")
    public SimulationTaskDispatcherPreSendProcessor(JobService jobService) {
        this.jobService = jobService;
    }

    @Override
    public TaskExecution process(TaskExecution taskExecution) {
        Job job = jobService.getJob(Validate.notNull(taskExecution.getJobId(), "jobId"));

        Map<String, ?> jobMetadata = job.getMetadata();

        taskExecution.putMetadata(
            MetadataConstants.DRY_RUN, MapUtils.getBoolean(jobMetadata, MetadataConstants.DRY_RUN, false));
        taskExecution.putMetadata(
            MetadataConstants.EDITOR_ENVIRONMENT,
            MapUtils.getBoolean(jobMetadata, MetadataConstants.EDITOR_ENVIRONMENT, false));

        return taskExecution;
    }

    @Override
    public boolean canProcess(TaskExecution taskExecution) {
        return true;
    }
}
