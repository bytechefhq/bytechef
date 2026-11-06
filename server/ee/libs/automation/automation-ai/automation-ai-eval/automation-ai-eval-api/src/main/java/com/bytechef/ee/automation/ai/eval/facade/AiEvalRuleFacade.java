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
 * @version ee
 * @author Ivica Cardic
 */
public interface AiEvalRuleFacade {

    AiEvalRule getEvalRule(long id);

    List<AiEvalRule> getEvalRulesByWorkspace(Long workspaceId);

    List<AiEvalExecution> getExecutionsByEvalRule(Long evalRuleId);

    List<AiEvalExecution> getExecutionsByTrace(Long traceId);

    AiEvalRule createInWorkspace(AiEvalRule evalRule, Long workspaceId);

    void deleteInWorkspace(long id);

    AiEvalRule update(AiEvalRule evalRule);

    List<EvalTemplate> listTemplates();

    AiEvalRule instantiateTemplate(
        String templateKey, Long workspaceId, Long projectId, String model, BigDecimal samplingRate);
}
