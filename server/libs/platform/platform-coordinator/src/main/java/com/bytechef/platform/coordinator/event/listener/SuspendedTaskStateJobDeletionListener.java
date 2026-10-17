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

package com.bytechef.platform.coordinator.event.listener;

import com.bytechef.atlas.coordinator.event.DeleteJobEvent;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.service.TaskStateService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;

/**
 * @author Ivica Cardic
 */
public class SuspendedTaskStateJobDeletionListener {

    private static final Logger log = LoggerFactory.getLogger(SuspendedTaskStateJobDeletionListener.class);

    private final @Nullable TaskStateService taskStateService;

    @SuppressFBWarnings("EI2")
    public SuspendedTaskStateJobDeletionListener(@Nullable TaskStateService taskStateService) {
        this.taskStateService = taskStateService;
    }

    @EventListener
    public void onDeleteJob(DeleteJobEvent deleteJobEvent) {
        if (taskStateService == null) {
            return;
        }

        Object jobResumeId = deleteJobEvent.getMetadata()
            .get(MetadataConstants.JOB_RESUME_ID);

        if (jobResumeId == null) {
            return;
        }

        try {
            taskStateService.delete(JobResumeId.parse(jobResumeId.toString()));
        } catch (RuntimeException runtimeException) {
            log.warn("Could not delete task state for deleted job {}", deleteJobEvent.getJobId(), runtimeException);
        }
    }
}
