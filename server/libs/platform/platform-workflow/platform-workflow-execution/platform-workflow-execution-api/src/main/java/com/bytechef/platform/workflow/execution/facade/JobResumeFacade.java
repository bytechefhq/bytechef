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

    enum JobResumeOutcome {
        OK, INVALID_ID, GONE
    }

    JobResumeOutcome resumeJob(String id, Map<String, Object> data);

    /**
     * Resumes a suspended job, exposing the resolved numeric job id so the caller can attach a stream sink for the
     * resumed turn's Server-Sent Events.
     *
     * <p>
     * The resumed turn runs asynchronously: {@code resumeJob} publishes a {@code ResumeJobEvent} onto the message
     * broker, the coordinator flips the job back to {@code STARTED}, and a worker continues the agent task. The
     * worker's {@code SseStreamTaskExecutionPostOutputProcessor} emits each streamed event onto the
     * {@code SSE_STREAM_EVENTS} message route, where {@code SseStreamBridgeRegistry} fans it out to bridges registered
     * by numeric job id. Because events can be emitted the instant the resumed task starts, a stream sink must be
     * registered <b>before</b> the resume is triggered — otherwise the first deltas are lost.
     * </p>
     *
     * <p>
     * To keep this facade transport- and registry-agnostic, sink registration is delegated to the caller via
     * {@code jobIdConsumer}: this method invokes it with the resolved numeric job id after the resume token has been
     * validated but before {@code ResumeJobEvent} is published. The caller (the webhook REST controller) uses that
     * window to register its {@code SseStreamBridge} with the {@code SseStreamBridgeRegistry}.
     * </p>
     *
     * <p>
     * Outcome semantics mirror {@link #resumeJob(String, Map)}: {@code INVALID_ID} for an unparseable or
     * token-mismatched id, {@code GONE} for a job that is no longer in {@code STOPPED} status, {@code OK} once the
     * resume event is published. On {@code INVALID_ID} / {@code GONE} {@code jobIdConsumer} is not invoked and the
     * resume is not triggered; the caller is expected to surface the failure on the SSE channel rather than leave the
     * stream hanging.
     * </p>
     *
     * @param id            the cryptographically signed resume id from the request path
     * @param data          the resume payload (e.g. the human's approval form submission)
     * @param jobIdConsumer invoked with the resolved numeric job id after validation, before the resume is triggered;
     *                      the caller registers its stream sink here
     * @return the resume outcome
     */
    JobResumeOutcome resumeJobStreaming(String id, Map<String, Object> data, LongConsumer jobIdConsumer);
}
