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

package com.bytechef.platform.workflow.coordinator.task.dispatcher;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.util.WorkflowTaskReferenceUtils;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherPreSendProcessor;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.platform.component.constant.MetadataConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Set;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Analyzes workflow definitions to determine which output property paths of the current task are referenced by
 * downstream tasks, and attaches those paths as metadata on the task execution.
 *
 * @author Ivica Cardic
 */
@Component
@Order(3)
class OutputReferenceTaskDispatcherPreSendProcessor implements TaskDispatcherPreSendProcessor {

    private static final Logger logger = LoggerFactory.getLogger(OutputReferenceTaskDispatcherPreSendProcessor.class);

    private final JobService jobService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    OutputReferenceTaskDispatcherPreSendProcessor(JobService jobService, WorkflowService workflowService) {
        this.jobService = jobService;
        this.workflowService = workflowService;
    }

    @Override
    public TaskExecution process(TaskExecution taskExecution) {
        Job job = jobService.getJob(Validate.notNull(taskExecution.getJobId(), "jobId"));

        Workflow workflow = workflowService.getWorkflow(job.getWorkflowId());

        Set<String> referencedOutputPaths = WorkflowTaskReferenceUtils.extractReferencedOutputPaths(
            workflow.getTasks(), taskExecution.getName());

        if (!referencedOutputPaths.isEmpty()) {
            taskExecution.putMetadata(MetadataConstants.OUTPUT_REFERENCE_PATHS, referencedOutputPaths);

            if (logger.isDebugEnabled()) {
                logger.debug(
                    "Task name='{}', type='{}' has {} referenced output paths: {}",
                    taskExecution.getName(), taskExecution.getType(), referencedOutputPaths.size(),
                    referencedOutputPaths);
            }
        }

        return taskExecution;
    }

    @Override
    public boolean canProcess(TaskExecution taskExecution) {
        return taskExecution.getName() != null;
    }
}
