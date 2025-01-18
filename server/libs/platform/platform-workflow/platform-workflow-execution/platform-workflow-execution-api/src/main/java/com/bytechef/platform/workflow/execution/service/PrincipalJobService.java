/*
 * Copyright 2023-present ByteChef Inc.
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
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.workflow.execution.domain.PrincipalJob;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;

/**
 * @author Ivica Cardic
 */
public interface PrincipalJobService {

    PrincipalJob create(long jobId, long instanceId, ModeType type);

    void deletePrincipalJobs(long jobId, ModeType type);

    Optional<Long> fetchLastJobId(long instanceId, ModeType type);

    Optional<Long> fetchJobPrincipalId(long jobId, ModeType type);

    long getJobPrincipalId(long jobId, ModeType type);

    List<Long> getJobIds(long principalId, ModeType type);

    Page<Long> getJobIds(
        Status status, Instant startDate, Instant endDate, List<Long> principalIds,
        ModeType type, List<String> workflowIds, int pageNumber);
}
