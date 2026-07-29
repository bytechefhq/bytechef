/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.alert.web.graphql;

import com.bytechef.ee.automation.workflow.alert.domain.WorkflowAlertRuleType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings({
    "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
})
public record WorkflowAlertRuleGraphQlInput(
    String name, @Nullable String workflowId, WorkflowAlertRuleType ruleType, BigDecimal threshold,
    @Nullable Integer windowMinutes, @Nullable Integer cooldownMinutes, List<Long> notificationIds,
    @Nullable Boolean enabled) {
}
