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

package com.bytechef.platform.workflow.coordinator.trigger.error;

import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessor;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * @author Monika Kušter
 */
@Component
public class TriggerErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(TriggerErrorHandler.class);

    private final ApplicationEventPublisher eventPublisher;
    private final JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;
    private final JobService jobService;
    private final PrincipalJobFacade principalJobFacade;

    @SuppressFBWarnings("EI")
    public TriggerErrorHandler(
        ApplicationEventPublisher eventPublisher, JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry,
        JobService jobService, PrincipalJobFacade principalJobFacade) {

        this.eventPublisher = eventPublisher;
        this.jobPrincipalAccessorRegistry = jobPrincipalAccessorRegistry;
        this.jobService = jobService;
        this.principalJobFacade = principalJobFacade;
    }

    public void handleError(TriggerExecution triggerExecution) {
        Assert.notNull(triggerExecution, "'triggerExecution' must not be null");
        Assert.notNull(triggerExecution.getId(), "'triggerExecution.id' must not be null");

        if (logger.isDebugEnabled()) {
            logger.debug("handleError: triggerExecution={}", triggerExecution);
        }

        WorkflowExecutionId workflowExecutionId = triggerExecution.getWorkflowExecutionId();

        Map<String, ?> inputMap = MapUtils.concat(
            (Map<String, Object>) getInputMap(workflowExecutionId), Map.of(triggerExecution.getName(), Map.of()));

        triggerExecution.addJobId(
            createFailedJob(
                getWorkflowId(workflowExecutionId), inputMap, workflowExecutionId.getJobPrincipalId(),
                getMetadataMap(workflowExecutionId), workflowExecutionId.getType(), triggerExecution.getStartDate(),
                triggerExecution.getEndDate()));
    }

    private long createFailedJob(
        String workflowId, Map<String, ?> inputMap, long jobPrincipalId, Map<String, ?> metadataMap, ModeType type,
        Instant startDate, Instant endDate) {

        Job job = principalJobFacade.createSyncJob(
            new JobParametersDTO(workflowId, inputMap, metadataMap), jobPrincipalId, type);

        job.setEndDate(endDate);
        job.setStartDate(startDate);
        job.setStatus(Job.Status.FAILED);

        job = jobService.update(job);

        long jobId = Validate.notNull(job.getId(), "id");

        eventPublisher.publishEvent(new JobStatusApplicationEvent(jobId, job.getStatus()));

        return jobId;
    }

    private Map<String, ?> getInputMap(WorkflowExecutionId workflowExecutionId) {
        JobPrincipalAccessor jobPrincipalAccessor = jobPrincipalAccessorRegistry.getJobPrincipalAccessor(
            workflowExecutionId.getType());

        return jobPrincipalAccessor.getInputMap(
            workflowExecutionId.getJobPrincipalId(), workflowExecutionId.getWorkflowUuid());
    }

    private String getWorkflowId(WorkflowExecutionId workflowExecutionId) {
        JobPrincipalAccessor jobPrincipalAccessor = jobPrincipalAccessorRegistry.getJobPrincipalAccessor(
            workflowExecutionId.getType());

        return jobPrincipalAccessor.getWorkflowId(
            workflowExecutionId.getJobPrincipalId(), workflowExecutionId.getWorkflowUuid());
    }

    private Map<String, ?> getMetadataMap(WorkflowExecutionId workflowExecutionId) {
        JobPrincipalAccessor jobPrincipalAccessor = jobPrincipalAccessorRegistry.getJobPrincipalAccessor(
            workflowExecutionId.getType());

        return jobPrincipalAccessor.getMetadataMap(workflowExecutionId.getJobPrincipalId());
    }
}
