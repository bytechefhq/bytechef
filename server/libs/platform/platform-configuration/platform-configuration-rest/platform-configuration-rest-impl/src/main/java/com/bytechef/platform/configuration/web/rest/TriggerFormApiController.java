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

package com.bytechef.platform.configuration.web.rest;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessor;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.dto.TriggerFormDTO;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnCoordinator
public class TriggerFormApiController {

    private final JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public TriggerFormApiController(
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry, WorkflowService workflowService) {

        this.jobPrincipalAccessorRegistry = jobPrincipalAccessorRegistry;
        this.workflowService = workflowService;
    }

    @GetMapping("/api/trigger-form/{id}")
    @SuppressWarnings("unchecked")
    public ResponseEntity<TriggerFormDTO> getTriggerForm(@PathVariable String id) {
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.parse(id);

        JobPrincipalAccessor jobPrincipalAccessor = jobPrincipalAccessorRegistry.getJobPrincipalAccessor(
            workflowExecutionId.getType());

        String workflowId = jobPrincipalAccessor.getWorkflowId(
            workflowExecutionId.getJobPrincipalId(), workflowExecutionId.getWorkflowUuid());

        WorkflowTrigger workflowTrigger = WorkflowTrigger
            .of(workflowService.getWorkflow(workflowId))
            .getFirst();

        Map<String, Object> parameters = (Map<String, Object>) workflowTrigger.getParameters();

        return ResponseEntity.ok(ConvertUtils.convertValue(parameters, TriggerFormDTO.class));
    }
}
