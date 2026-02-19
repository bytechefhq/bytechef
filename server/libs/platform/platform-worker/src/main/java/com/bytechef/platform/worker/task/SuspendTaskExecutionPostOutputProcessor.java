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

package com.bytechef.platform.worker.task;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.task.handler.TaskExecutionPostOutputProcessor;
import com.bytechef.component.definition.ActionDefinition.Suspend;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.scheduler.TriggerScheduler;
import com.bytechef.platform.workflow.execution.JobResumeId;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public class SuspendTaskExecutionPostOutputProcessor implements TaskExecutionPostOutputProcessor {

    private final @Nullable TriggerScheduler triggerScheduler;

    public SuspendTaskExecutionPostOutputProcessor(@Nullable TriggerScheduler triggerScheduler) {
        this.triggerScheduler = triggerScheduler;
    }

    @Override
    public Object process(TaskExecution taskExecution, Object output) {
        if (output instanceof Suspend suspend) {
            JobResumeId jobResumeId = JobResumeId.of(taskExecution.getJobId(), true);

            Instant expiresAt = suspend.expiresAt();

            if (expiresAt != null && triggerScheduler != null) {
                triggerScheduler.scheduleOneTimeTask(expiresAt, suspend.continueParameters(), taskExecution.getJobId());
            }

            taskExecution.putMetadata(MetadataConstants.JOB_RESUME_ID, jobResumeId.toString());
            taskExecution.putMetadata(MetadataConstants.SUSPEND, suspend);
        }

        return output;
    }
}
