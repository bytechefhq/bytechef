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
     * Cancels a run of the current tenant that is stopped waiting on an approval: the run becomes
     * {@link com.bytechef.atlas.execution.domain.Job.Status#CANCELLED} and its approval links stop working.
     *
     * @param jobId the id of the run
     * @return {@code true} when the run waited on an approval and was cancelled, {@code false} when it does not wait on
     *         one
     */
    boolean cancelApproval(long jobId);

    /**
     * Tells whether a resume id belongs to a run whose suspend waits for an approval decision, which only an explicit
     * submission may resolve.
     *
     * @param id the possibly signed resume id
     * @return {@code true} when the run waits, or waited, on an approval
     */
    boolean isApprovalResumeId(String id);

    /**
     * Resumes a job whose suspend deadline passed, without resume data. Does nothing unless the job is still stopped on
     * the suspend this resume id belongs to, so a deadline that outlived its suspend (answered, failed or stopped job)
     * cannot restart the job. Returns {@link JobResumeOutcome#NOT_YET_SUSPENDED} while the job has not reached the
     * suspend yet, which a deadline that passes before the suspend is stored can hit; the caller retries later. On
     * success the resume id is consumed, so a later answer gets {@link JobResumeOutcome#GONE}. A suspend that waits for
     * an approval is never resumed: its run fails with the expiry as the reason and {@link JobResumeOutcome#JOB_FAILED}
     * is returned.
     */
    JobResumeOutcome resumeExpiredJob(String id);

    /**
     * Resolves a legacy approval link; each approve/reject pair is one-shot. Only a verified signed token is accepted,
     * whatever {@code bytechef.approval.signed-token.required} says: an unsigned, tampered or expired token gets
     * {@link JobResumeOutcome#INVALID_ID}.
     *
     * @param token the signed approval token carried by the link
     * @return the outcome of the resume
     */
    JobResumeOutcome resumeApproval(String token);

    JobResumeOutcome resumeJob(String id, Map<String, Object> data);

    JobResumeOutcome resumeJob(String id, Map<String, Object> data, String approvedBy);

    JobResumeOutcome resumeJobStreaming(String id, Map<String, Object> data, LongConsumer jobIdConsumer);
}
