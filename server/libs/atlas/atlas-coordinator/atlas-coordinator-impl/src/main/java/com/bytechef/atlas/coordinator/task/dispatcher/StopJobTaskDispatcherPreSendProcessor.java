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

package com.bytechef.atlas.coordinator.task.dispatcher;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import org.apache.commons.lang3.Validate;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author Matija Petanjek
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class StopJobTaskDispatcherPreSendProcessor implements TaskDispatcherPreSendProcessor {

    private final JobService jobService;

    @SuppressFBWarnings("EI")
    public StopJobTaskDispatcherPreSendProcessor(JobService jobService) {
        this.jobService = jobService;
    }

    @Override
    public TaskExecution process(TaskExecution taskExecution) {
        Job job = jobService.getJob(Validate.notNull(taskExecution.getJobId(), "jobId"));

        if (job.getStatus() == Job.Status.STOPPED) {
            taskExecution.setEndDate(Instant.now());
            taskExecution.setStatus(TaskExecution.Status.CANCELLED);
        }

        return taskExecution;
    }

    @Override
    public boolean canProcess(TaskExecution taskExecution) {
        return true;
    }
}
