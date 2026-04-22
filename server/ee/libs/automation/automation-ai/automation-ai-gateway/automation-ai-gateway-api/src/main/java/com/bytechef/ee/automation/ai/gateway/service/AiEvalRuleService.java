/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiEvalRule;
import java.util.List;

/**
 * @version ee
 */
public interface AiEvalRuleService {

    AiEvalRule create(AiEvalRule evalRule);

    void delete(long id);

    AiEvalRule getEvalRule(long id);

    List<AiEvalRule> getEvalRulesByWorkspace(Long workspaceId);

    List<AiEvalRule> getEnabledEvalRulesByWorkspace(Long workspaceId);

    AiEvalRule update(AiEvalRule evalRule);
}
