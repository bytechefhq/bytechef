/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.agent.eval.facade;

import com.bytechef.ee.platform.ai.agent.eval.domain.AiAgentEvalResult;
import com.bytechef.ee.platform.ai.agent.eval.domain.AiAgentEvalRun;
import com.bytechef.ee.platform.ai.agent.eval.domain.AiAgentEvalTest;
import com.bytechef.ee.platform.ai.agent.eval.domain.AiAgentJudge;
import java.util.List;
import java.util.Optional;

/**
 * Tenant-admin-gated read surface for the AI-agent-eval GraphQL controller. The underlying eval services are also read
 * by the asynchronous {@code AiAgentEvalRunExecutor} (which runs with no user security context), so the authorization
 * cannot live on the shared services; it lives here, on the controller-facing read path. The executor keeps calling the
 * services directly.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiAgentEvalApiFacade {

    List<AiAgentJudge> getAgentJudges(String workflowId, String workflowNodeName);

    List<AiAgentEvalTest> getAgentEvalTests(String workflowId, String workflowNodeName);

    Optional<AiAgentEvalTest> fetchAgentEvalTest(long id);

    List<AiAgentEvalRun> getAgentEvalRuns(long agentEvalTestId);

    Optional<AiAgentEvalRun> fetchAgentEvalRun(long id);

    Optional<AiAgentEvalResult> fetchAgentEvalResult(long id);

    AiAgentEvalResult getAgentEvalResult(long id);
}
