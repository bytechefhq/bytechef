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

package com.bytechef.platform.workflow.execution.repository;

import com.bytechef.platform.workflow.execution.dto.WorkflowExecutionRowDTO;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Pages the rows of the executions list across the tables that back it: jobs from {@code principal_job} and
 * {@code job}, and trigger executions from {@code trigger_execution} that failed and produced no job.
 *
 * @author Ivica Cardic
 */
public interface WorkflowExecutionRowRepository {

    /**
     * @param status                            job status to match, or null for any
     * @param startDate                         day the job or trigger execution started, or null for any
     * @param endDate                           day the job or trigger execution ended, or null for any
     * @param principalIds                      deployments in scope
     * @param type                              platform type ordinal
     * @param workflowIds                       workflows in scope
     * @param onlyRootJobs                      whether subflow jobs are left out
     * @param failedTriggerWorkflowExecutionIds encoded workflow execution ids whose failed, jobless trigger executions
     *                                          join the page; empty leaves triggers out
     * @return both kinds of row ordered together by start date, newest first
     */
    Page<WorkflowExecutionRowDTO> findAll(
        Integer status, Instant startDate, Instant endDate, List<Long> principalIds, int type,
        List<String> workflowIds, boolean onlyRootJobs, List<String> failedTriggerWorkflowExecutionIds,
        Pageable pageable);
}
