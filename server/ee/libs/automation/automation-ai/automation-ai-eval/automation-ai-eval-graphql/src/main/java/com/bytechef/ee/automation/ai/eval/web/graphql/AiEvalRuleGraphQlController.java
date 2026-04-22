/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.eval.facade.AiEvalRuleFacade;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalExecution;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalRule;
import com.bytechef.ee.platform.ai.eval.template.EvalTemplate;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * Authorization is enforced on {@link AiEvalRuleFacade}, not here.
 *
 * @version ee
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class AiEvalRuleGraphQlController {

    private final AiEvalRuleFacade aiEvalRuleFacade;

    @SuppressFBWarnings("EI")
    AiEvalRuleGraphQlController(AiEvalRuleFacade aiEvalRuleFacade) {
        this.aiEvalRuleFacade = aiEvalRuleFacade;
    }

    @QueryMapping
    public AiEvalRule aiEvalRule(@Argument long id) {
        return aiEvalRuleFacade.getEvalRule(id);
    }

    @QueryMapping
    public List<AiEvalRule> aiEvalRules(@Argument Long workspaceId) {
        return aiEvalRuleFacade.getEvalRulesByWorkspace(workspaceId);
    }

    @QueryMapping
    public List<AiEvalExecution> aiEvalExecutions(@Argument Long evalRuleId) {
        return aiEvalRuleFacade.getExecutionsByEvalRule(evalRuleId);
    }

    @QueryMapping
    public List<AiEvalExecution> aiEvalExecutionsByTrace(@Argument Long traceId) {
        return aiEvalRuleFacade.getExecutionsByTrace(traceId);
    }

    @MutationMapping
    public AiEvalRule createAiEvalRule(
        @Argument Long workspaceId, @Argument String name, @Argument Long scoreConfigId,
        @Argument String promptTemplate, @Argument String model, @Argument Double samplingRate,
        @Argument String filters, @Argument Integer delaySeconds, @Argument boolean enabled,
        @Argument Long projectId) {

        AiEvalRule evalRule = new AiEvalRule(
            name, scoreConfigId, promptTemplate, model, BigDecimal.valueOf(samplingRate));

        evalRule.setEnabled(enabled);
        evalRule.setFilters(filters);
        evalRule.setDelaySeconds(delaySeconds);
        evalRule.setProjectId(projectId);

        return aiEvalRuleFacade.createInWorkspace(evalRule, workspaceId);
    }

    @MutationMapping
    public boolean deleteAiEvalRule(@Argument long id) {
        aiEvalRuleFacade.deleteInWorkspace(id);

        return true;
    }

    @MutationMapping
    public int runAiEvalRuleOnHistoricalTraces(
        @Argument Long ruleId, @Argument Long startDate, @Argument Long endDate) {

        return aiEvalRuleFacade.runOnHistoricalTraces(ruleId, startDate, endDate);
    }

    @QueryMapping
    public List<EvalTemplate> aiEvalTemplates() {
        return aiEvalRuleFacade.listTemplates();
    }

    @MutationMapping
    public AiEvalRule instantiateAiEvalTemplate(
        @Argument String templateKey, @Argument Long workspaceId, @Argument Long projectId,
        @Argument String model, @Argument Double samplingRate) {

        return aiEvalRuleFacade.instantiateTemplate(
            templateKey, workspaceId, projectId, model, BigDecimal.valueOf(samplingRate));
    }

    @MutationMapping
    public AiEvalRule updateAiEvalRule(
        @Argument long id, @Argument String name, @Argument Long scoreConfigId,
        @Argument String promptTemplate, @Argument String model, @Argument Double samplingRate,
        @Argument String filters, @Argument Integer delaySeconds, @Argument boolean enabled) {

        AiEvalRule evalRule = aiEvalRuleFacade.getEvalRule(id);

        evalRule.setName(name);
        evalRule.setScoreConfigId(scoreConfigId);
        evalRule.setPromptTemplate(promptTemplate);
        evalRule.setModel(model);
        evalRule.setSamplingRate(BigDecimal.valueOf(samplingRate));
        evalRule.setFilters(filters);
        evalRule.setDelaySeconds(delaySeconds);
        evalRule.setEnabled(enabled);

        return aiEvalRuleFacade.update(evalRule);
    }
}
