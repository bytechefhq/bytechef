/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.domain.AiEvalExecution;
import com.bytechef.ee.automation.ai.gateway.domain.AiEvalRule;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTrace;
import com.bytechef.ee.automation.ai.gateway.evaluation.AiEvalExecutor;
import com.bytechef.ee.automation.ai.gateway.service.AiEvalExecutionService;
import com.bytechef.ee.automation.ai.gateway.service.AiEvalRuleService;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityTraceService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.time.Instant;
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
class AiEvalRuleGraphQlController {

    private final AiEvalExecutionService aiEvalExecutionService;
    private final AiEvalExecutor aiEvalExecutor;
    private final AiEvalRuleService aiEvalRuleService;
    private final AiObservabilityTraceService aiObservabilityTraceService;

    @SuppressFBWarnings("EI")
    AiEvalRuleGraphQlController(
        AiEvalExecutionService aiEvalExecutionService,
        AiEvalExecutor aiEvalExecutor,
        AiEvalRuleService aiEvalRuleService,
        AiObservabilityTraceService aiObservabilityTraceService) {

        this.aiEvalExecutionService = aiEvalExecutionService;
        this.aiEvalExecutor = aiEvalExecutor;
        this.aiEvalRuleService = aiEvalRuleService;
        this.aiObservabilityTraceService = aiObservabilityTraceService;
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiEvalRule aiEvalRule(@Argument long id) {
        return aiEvalRuleService.getEvalRule(id);
    }

    @QueryMapping
    @PreAuthorize("@permissionService.hasWorkspaceRole(#workspaceId, 'VIEWER')")
    public List<AiEvalRule> aiEvalRules(@Argument Long workspaceId) {
        return aiEvalRuleService.getEvalRulesByWorkspace(workspaceId);
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<AiEvalExecution> aiEvalExecutions(@Argument Long evalRuleId) {
        return aiEvalExecutionService.getExecutionsByEvalRule(evalRuleId);
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<AiEvalExecution> aiEvalExecutionsByTrace(@Argument Long traceId) {
        return aiEvalExecutionService.getExecutionsByTrace(traceId);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiEvalRule createAiEvalRule(
        @Argument Long workspaceId, @Argument String name, @Argument Long scoreConfigId,
        @Argument String promptTemplate, @Argument String model, @Argument Double samplingRate,
        @Argument String filters, @Argument Integer delaySeconds, @Argument boolean enabled,
        @Argument Long projectId) {

        AiEvalRule evalRule = new AiEvalRule(
            workspaceId, name, scoreConfigId, promptTemplate, model,
            BigDecimal.valueOf(samplingRate));

        evalRule.setEnabled(enabled);
        evalRule.setFilters(filters);
        evalRule.setDelaySeconds(delaySeconds);
        evalRule.setProjectId(projectId);

        return aiEvalRuleService.create(evalRule);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public boolean deleteAiEvalRule(@Argument long id) {
        aiEvalRuleService.delete(id);

        return true;
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public int runAiEvalRuleOnHistoricalTraces(
        @Argument Long ruleId, @Argument Long startDate, @Argument Long endDate) {

        AiEvalRule evalRule = aiEvalRuleService.getEvalRule(ruleId);

        Instant windowStart = Instant.ofEpochMilli(startDate);
        Instant windowEnd = Instant.ofEpochMilli(endDate);

        List<AiObservabilityTrace> traces = aiObservabilityTraceService.getTracesByWorkspace(
            evalRule.getWorkspaceId(), windowStart, windowEnd);

        int queuedCount = 0;

        for (AiObservabilityTrace trace : traces) {
            aiEvalExecutor.evaluateTraceForRule(trace.getId(), evalRule.getId());

            queuedCount++;
        }

        return queuedCount;
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiEvalRule updateAiEvalRule(
        @Argument long id, @Argument String name, @Argument Long scoreConfigId,
        @Argument String promptTemplate, @Argument String model, @Argument Double samplingRate,
        @Argument String filters, @Argument Integer delaySeconds, @Argument boolean enabled) {

        AiEvalRule evalRule = aiEvalRuleService.getEvalRule(id);

        evalRule.setName(name);
        evalRule.setScoreConfigId(scoreConfigId);
        evalRule.setPromptTemplate(promptTemplate);
        evalRule.setModel(model);
        evalRule.setSamplingRate(BigDecimal.valueOf(samplingRate));
        evalRule.setFilters(filters);
        evalRule.setDelaySeconds(delaySeconds);
        evalRule.setEnabled(enabled);

        return aiEvalRuleService.update(evalRule);
    }
}
