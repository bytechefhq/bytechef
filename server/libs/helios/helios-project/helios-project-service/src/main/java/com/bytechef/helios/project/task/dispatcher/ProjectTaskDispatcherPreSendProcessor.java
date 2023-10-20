
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

package com.bytechef.helios.project.task.dispatcher;

import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherPreSendProcessor;
import com.bytechef.atlas.domain.Job;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.service.JobService;
import com.bytechef.helios.project.constant.ProjectConstants;
import com.bytechef.helios.project.service.ProjectInstanceService;
import com.bytechef.hermes.constant.MetadataConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * @author Ivica Cardic
 */
@Component
public class ProjectTaskDispatcherPreSendProcessor implements TaskDispatcherPreSendProcessor {

    private final JobService jobService;
    private final ProjectInstanceService projectInstanceService;

    @SuppressFBWarnings("EI")
    public ProjectTaskDispatcherPreSendProcessor(JobService jobService, ProjectInstanceService projectInstanceService) {
        this.jobService = jobService;
        this.projectInstanceService = projectInstanceService;
    }

    @Override
    @SuppressFBWarnings("NP")
    public TaskExecution process(TaskExecution taskExecution) {
        projectInstanceService.fetchJobProjectInstance(Objects.requireNonNull(taskExecution.getJobId()))
            .ifPresent(projectInstance -> taskExecution
                .putMetadata(MetadataConstants.INSTANCE_ID, projectInstance.getId())
                .putMetadata(MetadataConstants.INSTANCE_TYPE, ProjectConstants.PROJECT));

        Job job = jobService.getJob(Objects.requireNonNull(taskExecution.getJobId()));

        taskExecution.putMetadata(MetadataConstants.WORKFLOW_ID, job.getWorkflowId());

        return taskExecution;
    }
}
