/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiEvalExecution;
import java.time.Instant;
import java.util.List;

/**
 * @version ee
 */
public interface AiEvalExecutionService {

    AiEvalExecution create(AiEvalExecution evalExecution);

    void deleteOlderThan(Instant date);

    void deleteOlderThanByWorkspace(Instant date, Long workspaceId);

    List<AiEvalExecution> getExecutionsByEvalRule(Long evalRuleId);

    List<AiEvalExecution> getExecutionsByTrace(Long traceId);

    List<AiEvalExecution> getPendingExecutions();

    AiEvalExecution update(AiEvalExecution evalExecution);
}
