/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayBudget;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayBudgetEnforcementMode;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayBudgetPeriod;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayBudgetService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.regex.Pattern;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for managing AI LLM Gateway budgets.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class AiGatewayBudgetGraphQlController {

    private static final int MAX_AMOUNT_LENGTH = 20;
    private static final Pattern SAFE_DECIMAL_PATTERN = Pattern.compile("-?\\d+(\\.\\d+)?");

    private final AiGatewayBudgetService aiGatewayBudgetService;

    @SuppressFBWarnings("EI")
    AiGatewayBudgetGraphQlController(AiGatewayBudgetService aiGatewayBudgetService) {
        this.aiGatewayBudgetService = aiGatewayBudgetService;
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")
    public AiGatewayBudget aiGatewayBudget(@Argument long workspaceId) {
        return aiGatewayBudgetService.getBudgetByWorkspaceId(workspaceId)
            .orElse(null);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiGatewayBudget createAiGatewayBudget(@Argument CreateAiGatewayBudgetInput input) {
        AiGatewayBudget budget = new AiGatewayBudget(
            Long.valueOf(input.workspaceId()), parseSafeDecimal(input.amount()), input.period(),
            input.enforcementMode());

        if (input.alertThreshold() != null) {
            budget.setAlertThreshold(input.alertThreshold());
        }

        return aiGatewayBudgetService.create(budget);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public boolean deleteAiGatewayBudget(@Argument long id) {
        aiGatewayBudgetService.delete(id);

        return true;
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiGatewayBudget updateAiGatewayBudget(
        @Argument long id, @Argument UpdateAiGatewayBudgetInput input) {

        AiGatewayBudget budget = aiGatewayBudgetService.getBudget(id);

        if (input.alertThreshold() != null) {
            budget.setAlertThreshold(input.alertThreshold());
        }

        if (input.amount() != null) {
            budget.setAmount(parseSafeDecimal(input.amount()));
        }

        if (input.enabled() != null) {
            budget.setEnabled(input.enabled());
        }

        if (input.enforcementMode() != null) {
            budget.setEnforcementMode(input.enforcementMode());
        }

        if (input.period() != null) {
            budget.setPeriod(input.period());
        }

        return aiGatewayBudgetService.update(budget);
    }

    @SuppressFBWarnings("EI")
    public record CreateAiGatewayBudgetInput(
        Integer alertThreshold, String amount, AiGatewayBudgetEnforcementMode enforcementMode,
        AiGatewayBudgetPeriod period, String workspaceId) {
    }

    @SuppressFBWarnings("EI")
    public record UpdateAiGatewayBudgetInput(
        Integer alertThreshold, String amount, Boolean enabled,
        AiGatewayBudgetEnforcementMode enforcementMode, AiGatewayBudgetPeriod period) {
    }

    private static BigDecimal parseSafeDecimal(String amount) {
        if (amount == null || amount.length() > MAX_AMOUNT_LENGTH || !SAFE_DECIMAL_PATTERN.matcher(amount)
            .matches()) {
            throw new IllegalArgumentException(
                "Invalid budget amount: must be a plain decimal number with at most " + MAX_AMOUNT_LENGTH +
                    " characters (no scientific notation)");
        }

        return new BigDecimal(amount);
    }
}
