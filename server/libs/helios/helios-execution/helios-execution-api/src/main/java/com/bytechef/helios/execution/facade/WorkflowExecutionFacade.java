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

package com.bytechef.helios.execution.facade;

import com.bytechef.helios.execution.dto.TestConnectionDTO;
import com.bytechef.helios.execution.dto.WorkflowExecutionDTO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;

/**
 * @author Ivica Cardic
 */
public interface WorkflowExecutionFacade {

    WorkflowExecutionDTO getWorkflowExecution(long id);

    Page<WorkflowExecutionDTO> getWorkflowExecutions(
        String jobStatus, LocalDateTime jobStartDate, LocalDateTime jobEndDate, Long projectId, Long projectInstanceId,
        String workflowId, int pageNumber);

    WorkflowExecutionDTO testWorkflow(
        String workflowId, Map<String, Object> inputs, List<TestConnectionDTO> testConnectionDTOs);
}
