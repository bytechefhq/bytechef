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
import com.bytechef.ee.platform.ai.agent.eval.service.AiAgentEvalResultService;
import com.bytechef.ee.platform.ai.agent.eval.service.AiAgentEvalRunService;
import com.bytechef.ee.platform.ai.agent.eval.service.AiAgentEvalTestService;
import com.bytechef.ee.platform.ai.agent.eval.service.AiAgentJudgeService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * Applies a tenant-admin gate to every AI-agent-eval read consumed by the GraphQL controller, then delegates to the
 * shared eval services. The asynchronous {@code AiAgentEvalRunExecutor} keeps calling those services directly and is
 * unaffected.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
class AiAgentEvalApiFacadeImpl implements AiAgentEvalApiFacade {

    private final AiAgentEvalResultService agentEvalResultService;
    private final AiAgentEvalRunService agentEvalRunService;
    private final AiAgentEvalTestService agentEvalTestService;
    private final AiAgentJudgeService agentJudgeService;

    @SuppressFBWarnings("EI")
    AiAgentEvalApiFacadeImpl(
        AiAgentEvalResultService agentEvalResultService, AiAgentEvalRunService agentEvalRunService,
        AiAgentEvalTestService agentEvalTestService, AiAgentJudgeService agentJudgeService) {

        this.agentEvalResultService = agentEvalResultService;
        this.agentEvalRunService = agentEvalRunService;
        this.agentEvalTestService = agentEvalTestService;
        this.agentJudgeService = agentJudgeService;
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public List<AiAgentJudge> getAgentJudges(String workflowId, String workflowNodeName) {
        return agentJudgeService.getAgentJudges(workflowId, workflowNodeName);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public List<AiAgentEvalTest> getAgentEvalTests(String workflowId, String workflowNodeName) {
        return agentEvalTestService.getAgentEvalTests(workflowId, workflowNodeName);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public Optional<AiAgentEvalTest> fetchAgentEvalTest(long id) {
        return agentEvalTestService.fetchAgentEvalTest(id);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public List<AiAgentEvalRun> getAgentEvalRuns(long agentEvalTestId) {
        return agentEvalRunService.getAgentEvalRuns(agentEvalTestId);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public Optional<AiAgentEvalRun> fetchAgentEvalRun(long id) {
        return agentEvalRunService.fetchAgentEvalRun(id);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public Optional<AiAgentEvalResult> fetchAgentEvalResult(long id) {
        return agentEvalResultService.fetchAgentEvalResult(id);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public AiAgentEvalResult getAgentEvalResult(long id) {
        return agentEvalResultService.getAgentEvalResult(id);
    }
}
