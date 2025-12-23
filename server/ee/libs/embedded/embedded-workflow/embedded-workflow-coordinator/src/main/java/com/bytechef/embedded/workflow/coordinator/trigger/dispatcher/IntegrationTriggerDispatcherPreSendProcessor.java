/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.embedded.workflow.coordinator.trigger.dispatcher;

import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.embedded.workflow.coordinator.AbstractDispatcherPreSendProcessor;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.coordinator.trigger.dispatcher.TriggerDispatcherPreSendProcessor;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@Order(3)
@ConditionalOnEEVersion
public class IntegrationTriggerDispatcherPreSendProcessor extends AbstractDispatcherPreSendProcessor
    implements TriggerDispatcherPreSendProcessor {

    private final IntegrationWorkflowService integrationWorkflowService;
    private final JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;

    @SuppressFBWarnings("EI")
    public IntegrationTriggerDispatcherPreSendProcessor(
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationWorkflowService integrationWorkflowService,
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry) {

        super(integrationInstanceConfigurationWorkflowService);

        this.integrationWorkflowService = integrationWorkflowService;
        this.jobPrincipalAccessorRegistry = jobPrincipalAccessorRegistry;
    }

    @Override
    public TriggerExecution process(TriggerExecution triggerExecution) {
        WorkflowExecutionId workflowExecutionId = triggerExecution.getWorkflowExecutionId();

        String workflowId = integrationWorkflowService.getWorkflowId(
            workflowExecutionId.getJobPrincipalId(), workflowExecutionId.getWorkflowUuid());

        Map<String, Long> connectionIdMap = getConnectionIdMap(
            workflowExecutionId.getJobPrincipalId(), workflowId, triggerExecution.getName());

        if (!connectionIdMap.isEmpty()) {
            triggerExecution.putMetadata(MetadataConstants.CONNECTION_IDS, connectionIdMap);
        }

        int environmentId = (int) jobPrincipalAccessorRegistry
            .getJobPrincipalAccessor(PlatformType.EMBEDDED)
            .getEnvironmentId(workflowExecutionId.getJobPrincipalId());
        triggerExecution.putMetadata(MetadataConstants.ENVIRONMENT_ID, environmentId);

        return triggerExecution;
    }

    @Override
    public boolean canProcess(TriggerExecution triggerExecution) {
        WorkflowExecutionId workflowExecutionId = triggerExecution.getWorkflowExecutionId();

        return workflowExecutionId.getType() == PlatformType.EMBEDDED;
    }
}
