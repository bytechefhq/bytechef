/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.service;

import com.bytechef.ee.platform.ai.eval.domain.AiEvalRule;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalRuleTarget;
import java.util.List;

/**
 * @author Ivica Cardic
 * @version ee
 */
public interface AiEvalRuleService {

    AiEvalRule create(AiEvalRule evalRule);

    void delete(long id);

    AiEvalRule getEvalRule(long id);

    List<AiEvalRule> getEvalRules(List<Long> ids);

    List<AiEvalRule> getEnabledEvalRulesByIdsAndTarget(List<Long> ids, AiEvalRuleTarget target);

    AiEvalRule update(AiEvalRule evalRule);
}
