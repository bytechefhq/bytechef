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

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.platform.constant.PlatformType;

/**
 * @author Ivica Cardic
 */
public interface PrincipalJobFacade {

    long createChildJob(long parentJobId, JobParametersDTO jobParametersDTO, PlatformType platformType);

    long createJob(JobParametersDTO jobParametersDTO, long jobPrincipalId, PlatformType type);

    /**
     * Creates a top-level job (no parent task execution) linked to the same principal instance as
     * {@code referenceJobId}. Used by the agent-tool sub-workflow bridge, where the sub-workflow must be independently
     * resumable and therefore cannot be a child job.
     *
     * @param referenceJobId   the job whose principal the new job is linked to
     * @param jobParametersDTO the new job's parameters
     * @param platformType     the platform type
     * @return the created job id
     */
    long createPrincipalLinkedJob(long referenceJobId, JobParametersDTO jobParametersDTO, PlatformType platformType);

    /**
     * Creates a persisted job row without dispatching it to the coordinator. Used to record a job (e.g. a failed
     * trigger) in history; the job is not executed.
     */
    Job createJobWithoutDispatch(JobParametersDTO jobParametersDTO, long jobPrincipalId, PlatformType type);
}
