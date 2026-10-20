/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.workflow.execution.security;

import com.bytechef.automation.configuration.security.ResourceEnvironmentResolver;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class EmbeddedTriggerExecutionEnvironmentResolver implements ResourceEnvironmentResolver {

    private final EmbeddedPrincipalEnvironmentResolver embeddedPrincipalEnvironmentResolver;
    private final TriggerExecutionService triggerExecutionService;

    @SuppressFBWarnings("EI")
    public EmbeddedTriggerExecutionEnvironmentResolver(
        EmbeddedPrincipalEnvironmentResolver embeddedPrincipalEnvironmentResolver,
        TriggerExecutionService triggerExecutionService) {

        this.embeddedPrincipalEnvironmentResolver = embeddedPrincipalEnvironmentResolver;
        this.triggerExecutionService = triggerExecutionService;
    }

    @Override
    public String resourceType() {
        return "TriggerExecution";
    }

    @Override
    public Optional<Environment> fetchEnvironment(Serializable id) {
        if (!(id instanceof Number number)) {
            return Optional.empty();
        }

        List<TriggerExecution> triggerExecutions = triggerExecutionService.getTriggerExecutions(
            List.of(number.longValue()));

        if (triggerExecutions.isEmpty()) {
            return Optional.empty();
        }

        TriggerExecution triggerExecution = triggerExecutions.getFirst();
        WorkflowExecutionId workflowExecutionId = triggerExecution.getWorkflowExecutionId();

        if (workflowExecutionId == null || workflowExecutionId.getType() != PlatformType.EMBEDDED) {
            return Optional.empty();
        }

        String workflowUuid = workflowExecutionId.getWorkflowUuid();

        if (workflowUuid == null) {
            return Optional.empty();
        }

        return embeddedPrincipalEnvironmentResolver.fetchInstanceEnvironment(
            workflowExecutionId.getJobPrincipalId(),
            integrationWorkflow -> workflowUuid.equalsIgnoreCase(integrationWorkflow.getUuidAsString()));
    }
}
