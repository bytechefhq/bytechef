/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.facade;

import com.bytechef.ee.platform.ai.eval.domain.AiEvalExecution;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalRule;
import com.bytechef.ee.platform.ai.eval.template.EvalTemplate;
import java.math.BigDecimal;
import java.util.List;

/**
 * Facade for AI eval rule operations. Hosts the authorization guards so they apply to every caller of the facade rather
 * than only the GraphQL entry point, and keeps them off the shared {@code AiEvalRuleService}/{@code AiEvalExecutor}
 * which the gateway evaluation data plane relies on.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiEvalRuleFacade {

    AiEvalRule getEvalRule(long id);

    List<AiEvalRule> getEvalRulesByWorkspace(Long workspaceId);

    List<AiEvalExecution> getExecutionsByEvalRule(Long evalRuleId);

    List<AiEvalExecution> getExecutionsByTrace(Long traceId);

    AiEvalRule createInWorkspace(AiEvalRule evalRule, Long workspaceId);

    void deleteInWorkspace(long id);

    int runOnHistoricalTraces(Long ruleId, long startDate, long endDate);

    AiEvalRule update(AiEvalRule evalRule);

    List<EvalTemplate> listTemplates();

    AiEvalRule instantiateTemplate(
        String templateKey, Long workspaceId, Long projectId, String model, BigDecimal samplingRate);
}
