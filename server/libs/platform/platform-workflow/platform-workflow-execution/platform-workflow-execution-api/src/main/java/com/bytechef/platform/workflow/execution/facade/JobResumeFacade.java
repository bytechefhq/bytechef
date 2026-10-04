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

package com.bytechef.platform.workflow.execution.facade;

import java.util.Map;
import java.util.function.LongConsumer;

/**
 * @author Ivica Cardic
 */
public interface JobResumeFacade {

    /**
     * The outcome of a resume. Only {@link #resumeJobStreaming} returns {@link #STREAMING_NOT_ALLOWED}.
     */
    enum JobResumeOutcome {
        OK, INVALID_ID, GONE, JOB_FAILED, NOT_YET_SUSPENDED, STREAMING_NOT_ALLOWED
    }

    /**
     * Resumes a job whose suspend deadline passed, without resume data. Does nothing unless the job is still stopped on
     * the suspend this resume id belongs to, so a deadline that outlived its suspend (answered, failed or stopped job)
     * cannot restart the job. Returns {@link JobResumeOutcome#NOT_YET_SUSPENDED} while the job has not reached the
     * suspend yet, which a deadline that passes before the suspend is stored can hit; the caller retries later. On
     * success the resume id is consumed, so a later answer gets {@link JobResumeOutcome#GONE}.
     */
    JobResumeOutcome resumeExpiredJob(String id);

    JobResumeOutcome resumeJob(String id, Map<String, Object> data);

    JobResumeOutcome resumeJobStreaming(String id, Map<String, Object> data, LongConsumer jobIdConsumer);
}
