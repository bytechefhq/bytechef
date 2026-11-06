/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.facade;

import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityAlertRule;
import java.util.List;

/**
 * @version ee
 * @author Ivica Cardic
 */
public interface AiObservabilityAlertRuleFacade {

    List<AiObservabilityAlertRule> getAlertRulesByWorkspace(Long workspaceId);
}
