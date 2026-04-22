/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertEvent;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertEventStatus;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertRule;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityAlertEventService;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityAlertRuleService;
import com.bytechef.ee.automation.ai.gateway.web.graphql.authorization.WorkspaceAuthorization;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class AiObservabilityAlertEventGraphQlController {

    private final AiObservabilityAlertEventService aiObservabilityAlertEventService;
    private final AiObservabilityAlertRuleService aiObservabilityAlertRuleService;
    private final WorkspaceAuthorization workspaceAuthorization;

    @SuppressFBWarnings("EI")
    AiObservabilityAlertEventGraphQlController(
        AiObservabilityAlertEventService aiObservabilityAlertEventService,
        AiObservabilityAlertRuleService aiObservabilityAlertRuleService,
        WorkspaceAuthorization workspaceAuthorization) {

        this.aiObservabilityAlertEventService = aiObservabilityAlertEventService;
        this.aiObservabilityAlertRuleService = aiObservabilityAlertRuleService;
        this.workspaceAuthorization = workspaceAuthorization;
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<AiObservabilityAlertEvent> aiObservabilityAlertEvents(@Argument Long alertRuleId) {
        AiObservabilityAlertRule alertRule = aiObservabilityAlertRuleService.getAlertRule(alertRuleId);

        workspaceAuthorization.requireWorkspaceRole(alertRule.getWorkspaceId(), "VIEWER");

        return aiObservabilityAlertEventService.getAlertEventsByRule(alertRuleId);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public AiObservabilityAlertEvent acknowledgeAiObservabilityAlertEvent(@Argument long id) {
        AiObservabilityAlertEvent alertEvent = aiObservabilityAlertEventService.getAlertEvent(id);

        AiObservabilityAlertRule alertRule = aiObservabilityAlertRuleService.getAlertRule(alertEvent.getAlertRuleId());

        workspaceAuthorization.requireWorkspaceRole(alertRule.getWorkspaceId(), "EDITOR");

        alertEvent.setStatus(AiObservabilityAlertEventStatus.ACKNOWLEDGED);

        return aiObservabilityAlertEventService.update(alertEvent);
    }

}
