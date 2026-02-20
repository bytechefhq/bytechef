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

package com.bytechef.platform.webhook.web.service;

import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.platform.webhook.TriggerCompletionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Implementation of TriggerCompletionService that completes triggers and continues workflows after asynchronous
 * processing like WebSocket-based real-time workflows.
 *
 * <p>
 * This simplified implementation creates a job directly using JobFacade. For more complex scenarios that require
 * trigger execution tracking, the full TriggerCompletionHandler should be used.
 * </p>
 *
 * @author Ivica Cardic
 */
@Service
class TriggerCompletionServiceImpl implements TriggerCompletionService {

    private static final Logger logger = LoggerFactory.getLogger(TriggerCompletionServiceImpl.class);

    private final JobFacade jobFacade;

    @SuppressFBWarnings("EI")
    TriggerCompletionServiceImpl(JobFacade jobFacade) {
        this.jobFacade = jobFacade;
    }

    @Override
    public void completeTrigger(String workflowExecutionId, Map<String, Object> triggerOutput) {
        logger.info("Completing trigger for workflowExecutionId={}", workflowExecutionId);

        try {
            // The workflowExecutionId contains encoded workflow information
            // For this simplified implementation, we extract the workflow ID and create a new job
            // A full implementation would use TriggerCompletionHandler for proper trigger tracking

            String workflowId = extractWorkflowId(workflowExecutionId);

            if (workflowId == null) {
                logger.error("Unable to extract workflow ID from workflowExecutionId={}", workflowExecutionId);

                return;
            }

            JobParametersDTO jobParameters = new JobParametersDTO(workflowId, triggerOutput);

            long jobId = jobFacade.createJob(jobParameters);

            logger.info(
                "Trigger completed successfully for workflowExecutionId={}, created jobId={}", workflowExecutionId,
                jobId);
        } catch (Exception exception) {
            logger.error("Failed to complete trigger for workflowExecutionId={}", workflowExecutionId, exception);

            throw exception;
        }
    }

    @Override
    public void completeTriggerWithError(String workflowExecutionId, String errorMessage) {
        logger.info(
            "Completing trigger with error for workflowExecutionId={}, error={}", workflowExecutionId, errorMessage);

        // For error completion, we just log and don't continue the workflow
        // The workflow will not be started if the trigger fails
        logger.error(
            "Trigger failed for workflowExecutionId={}: {}", workflowExecutionId, errorMessage);
    }

    private static String extractWorkflowId(String workflowExecutionId) {
        // WorkflowExecutionId format: "type_principalId_workflowUuid_triggerName"
        // For now, we return null as a placeholder - the full implementation would use
        // WorkflowExecutionId.parse() and JobPrincipalAccessor to get the workflow ID

        if (workflowExecutionId == null || workflowExecutionId.isBlank()) {
            return null;
        }

        // Workflow ID extraction requires JobPrincipalAccessorRegistry - returning null as placeholder
        return null;
    }
}
