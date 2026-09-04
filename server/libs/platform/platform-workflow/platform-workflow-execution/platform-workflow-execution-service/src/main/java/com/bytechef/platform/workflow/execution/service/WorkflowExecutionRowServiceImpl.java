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
import com.bytechef.platform.workflow.execution.repository.PrincipalJobRepository;
import com.bytechef.platform.workflow.execution.repository.WorkflowExecutionRowRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class WorkflowExecutionRowServiceImpl implements WorkflowExecutionRowService {

    private final WorkflowExecutionRowRepository workflowExecutionRowRepository;

    @SuppressFBWarnings("EI")
    public WorkflowExecutionRowServiceImpl(WorkflowExecutionRowRepository workflowExecutionRowRepository) {
        this.workflowExecutionRowRepository = workflowExecutionRowRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WorkflowExecutionRowDTO> getWorkflowExecutionRows(
        Status status, Instant startDate, Instant endDate, List<Long> principalIds, PlatformType type,
        List<String> workflowIds, boolean onlyRootJobs, List<String> failedTriggerWorkflowExecutionIds,
        int pageNumber) {

        return workflowExecutionRowRepository.findAll(
            status == null ? null : status.ordinal(), startDate, endDate, principalIds, type.ordinal(), workflowIds,
            onlyRootJobs, failedTriggerWorkflowExecutionIds,
            PageRequest.of(pageNumber, PrincipalJobRepository.DEFAULT_PAGE_SIZE));
    }
}
