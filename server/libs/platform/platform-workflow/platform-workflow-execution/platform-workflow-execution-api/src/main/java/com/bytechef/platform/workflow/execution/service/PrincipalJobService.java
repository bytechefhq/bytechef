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

package com.bytechef.platform.workflow.execution.service;

import com.bytechef.atlas.execution.domain.Job.Status;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.domain.PrincipalJob;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;

/**
 * @author Ivica Cardic
 */
public interface PrincipalJobService {

    PrincipalJob create(long jobId, long principalId, PlatformType type);

    void deletePrincipalJobs(long jobId, PlatformType type);

    Optional<Long> fetchLastJobId(long principalId, PlatformType type);

    Optional<Long> fetchLastWorkflowJobId(long principalId, List<String> workflowIds, PlatformType type);

    Optional<Long> fetchJobPrincipalId(long jobId, PlatformType type);

    long getJobPrincipalId(long jobId, PlatformType type);

    List<Long> getJobIds(long principalId, PlatformType type);

    Page<Long> getJobIds(
        Status status, Instant startDate, Instant endDate, List<Long> principalIds, PlatformType type,
        List<String> workflowIds, int pageNumber);
}
