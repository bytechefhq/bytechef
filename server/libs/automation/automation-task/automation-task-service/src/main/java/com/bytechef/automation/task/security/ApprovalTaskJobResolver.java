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

package com.bytechef.automation.task.security;

import com.bytechef.platform.workflow.execution.JobResumeId;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Resolves the job an approval task is raised for, so a guard can require a scope on that job before the task exists.
 * An absent or unparsable job resume id resolves to {@code -1}, which no job has, so the scope check refuses it.
 *
 * @author Ivica Cardic
 */
@Component("approvalTaskJobResolver")
public class ApprovalTaskJobResolver {

    public long getJobId(@Nullable String jobResumeId) {
        if (jobResumeId == null) {
            return -1;
        }

        try {
            JobResumeId parsedJobResumeId = JobResumeId.parse(jobResumeId);

            return parsedJobResumeId.getJobId();
        } catch (IllegalArgumentException illegalArgumentException) {
            return -1;
        }
    }
}
