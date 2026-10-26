/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailViolation;
import com.bytechef.ee.platform.ai.guardrails.service.AiGuardrailViolationService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
class AiGuardrailViolationGraphQlController {

    private static final int DEFAULT_LIMIT = 100;

    private final AiGuardrailViolationService aiGuardrailViolationService;

    @SuppressFBWarnings("EI")
    AiGuardrailViolationGraphQlController(AiGuardrailViolationService aiGuardrailViolationService) {
        this.aiGuardrailViolationService = aiGuardrailViolationService;
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or (#workspaceId != null "
        + "&& hasPermission(#workspaceId, 'Workspace', 'AI_GATEWAY_VIEW'))")
    public List<AiGuardrailViolation> aiGuardrailViolations(
        @Argument @Nullable Long workspaceId, @Argument @Nullable Integer limit) {

        return aiGuardrailViolationService.getViolations(workspaceId, limit == null ? DEFAULT_LIMIT : limit);
    }
}
