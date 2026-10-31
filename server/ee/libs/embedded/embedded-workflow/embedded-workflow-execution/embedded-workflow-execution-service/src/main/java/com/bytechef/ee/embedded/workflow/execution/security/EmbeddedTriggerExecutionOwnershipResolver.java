/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.workflow.execution.security;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class EmbeddedTriggerExecutionOwnershipResolver implements ResourceOwnershipResolver {

    private final TriggerExecutionService triggerExecutionService;

    @SuppressFBWarnings("EI")
    public EmbeddedTriggerExecutionOwnershipResolver(TriggerExecutionService triggerExecutionService) {
        this.triggerExecutionService = triggerExecutionService;
    }

    @Override
    public String resourceType() {
        return "TriggerExecution";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        List<TriggerExecution> triggerExecutions = triggerExecutionService.getTriggerExecutions(List.of(id));

        if (triggerExecutions.isEmpty()) {
            return ResourceOwner.unknown();
        }

        TriggerExecution triggerExecution = triggerExecutions.getFirst();
        WorkflowExecutionId workflowExecutionId = triggerExecution.getWorkflowExecutionId();

        if (workflowExecutionId != null && workflowExecutionId.getType() == PlatformType.EMBEDDED) {
            return ResourceOwner.ofEmbedded();
        }

        return ResourceOwner.unknown();
    }
}
