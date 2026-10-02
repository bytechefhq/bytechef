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

    JobResumeOutcome resumeJob(String id, Map<String, Object> data);

    JobResumeOutcome resumeJobStreaming(String id, Map<String, Object> data, LongConsumer jobIdConsumer);
}
