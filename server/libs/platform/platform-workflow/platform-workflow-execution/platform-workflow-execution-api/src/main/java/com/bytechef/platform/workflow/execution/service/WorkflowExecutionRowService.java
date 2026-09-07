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
import com.bytechef.platform.workflow.execution.dto.WorkflowExecutionRowDTO;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Pages the executions list: jobs, unioned with the failed trigger executions that produced no job and whose encoded
 * workflow execution id is in the given set. Both kinds are ordered together by start date, newest first; an empty id
 * set leaves triggers out.
 *
 * @author Ivica Cardic
 */
public interface WorkflowExecutionRowService {

    Page<WorkflowExecutionRowDTO> getWorkflowExecutionRows(
        Status status, Instant startDate, Instant endDate, List<Long> principalIds, PlatformType type,
        List<String> workflowIds, boolean onlyRootJobs, List<String> failedTriggerWorkflowExecutionIds,
        int pageNumber);
}
