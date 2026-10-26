/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.service;

import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailCustomRule;
import java.util.List;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiGuardrailCustomRuleService {

    AiGuardrailCustomRule create(AiGuardrailCustomRule customRule);

    AiGuardrailCustomRule updatePattern(long id, long workspaceId, String pattern);

    AiGuardrailCustomRule setEnabled(long id, long workspaceId, boolean enabled);

    void delete(long id, long workspaceId);

    Optional<AiGuardrailCustomRule> fetchRule(long id, long workspaceId);

    List<AiGuardrailCustomRule> getRules(long workspaceId);

    List<AiGuardrailCustomRule> getEnabledRules(long workspaceId);
}
