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

package com.bytechef.platform.webhook.web.websocket;

import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Creates a continuation job to resume the main workflow after a call or WebSocket session ends. Passes the after-call
 * data (e.g., Twilio callback params or WebSocket close info) as the trigger output so the workflow receives it as
 * input.
 *
 * @author Ivica Cardic
 */
@Component
public class WorkflowContinuationHelper {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowContinuationHelper.class);

    private final JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;
    private final PrincipalJobFacade principalJobFacade;

    @SuppressFBWarnings("EI")
    WorkflowContinuationHelper(
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry, PrincipalJobFacade principalJobFacade) {

        this.jobPrincipalAccessorRegistry = jobPrincipalAccessorRegistry;
        this.principalJobFacade = principalJobFacade;
    }

    /**
     * Creates a continuation job for the main workflow identified by the given workflow execution ID string. The
     * afterCallData map is set as the trigger output so the workflow can access it.
     *
     * @param workflowExecutionIdString the encoded workflow execution ID from the CallSession
     * @param afterCallData             the data to pass as the trigger output (e.g., Twilio callback params)
     */
    public void createContinuationJob(String workflowExecutionIdString, Map<String, Object> afterCallData) {
        try {
            WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.parse(workflowExecutionIdString);

            JobPrincipalAccessor jobPrincipalAccessor = jobPrincipalAccessorRegistry.getJobPrincipalAccessor(
                workflowExecutionId.getType());

            String workflowId = jobPrincipalAccessor.getWorkflowId(
                workflowExecutionId.getJobPrincipalId(), workflowExecutionId.getWorkflowUuid());

            Map<String, Object> inputs = new HashMap<>(
                jobPrincipalAccessor.getInputMap(
                    workflowExecutionId.getJobPrincipalId(), workflowExecutionId.getWorkflowUuid()));

            inputs.put(workflowExecutionId.getTriggerName(), afterCallData);

            long jobId = TenantContext.callWithTenantId(
                workflowExecutionId.getTenantId(),
                () -> principalJobFacade.createJob(
                    new JobParametersDTO(workflowId, inputs),
                    workflowExecutionId.getJobPrincipalId(),
                    workflowExecutionId.getType()));

            logger.info(
                "Created continuation job: jobId={}, workflowExecutionId={}", jobId, workflowExecutionIdString);
        } catch (Exception exception) {
            logger.error(
                "Failed to create continuation job: workflowExecutionId={}", workflowExecutionIdString, exception);
        }
    }
}
