/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade.WorkflowValidationResult;
import com.bytechef.tenant.TenantContext;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
class ConnectedUserWorkflowValidator {

    private static final Logger log = LoggerFactory.getLogger(ConnectedUserWorkflowValidator.class);

    private final Cache<String, Integer> errorCountCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(1))
        .maximumSize(10_000)
        .build();
    private final WorkflowService workflowService;
    private final WorkflowValidatorFacade workflowValidatorFacade;

    @SuppressFBWarnings("EI")
    ConnectedUserWorkflowValidator(WorkflowService workflowService, WorkflowValidatorFacade workflowValidatorFacade) {
        this.workflowService = workflowService;
        this.workflowValidatorFacade = workflowValidatorFacade;
    }

    @Nullable
    String getAttentionReason(String workflowId, Environment environment) {
        try {
            int workflowErrorCount = getErrorCount(workflowId, environment);

            return workflowErrorCount > 0
                ? ConnectedUserReferenceAttentionResolver.INVALID_WORKFLOW_PREFIX + workflowErrorCount : null;
        } catch (RuntimeException exception) {
            log.warn("Workflow id={} could not be validated: {}", workflowId, exception.getMessage());

            if (log.isDebugEnabled()) {
                log.debug("Workflow id={} could not be validated", workflowId, exception);
            }

            return null;
        }
    }

    int getErrorCount(String workflowId, Environment environment) {
        Workflow workflow = workflowService.getWorkflow(workflowId);

        String cacheKey = String.join(
            ":", TenantContext.getCurrentTenantId(), workflowId, String.valueOf(workflow.getVersion()),
            String.valueOf(environment.ordinal()));

        return errorCountCache.get(cacheKey, key -> countErrors(workflow, environment));
    }

    private int countErrors(Workflow workflow, Environment environment) {
        WorkflowValidationResult workflowValidationResult = workflowValidatorFacade.validateWorkflow(
            workflow.getDefinition(), environment.ordinal());

        return workflowValidationResult.errors()
            .size();
    }
}
