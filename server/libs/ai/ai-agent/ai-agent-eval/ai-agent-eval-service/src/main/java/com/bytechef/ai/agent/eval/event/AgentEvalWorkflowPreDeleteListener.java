/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.ai.agent.eval.event;

import com.bytechef.ai.agent.eval.service.AgentEvalTestService;
import com.bytechef.ai.agent.eval.service.AgentJudgeService;
import com.bytechef.platform.configuration.workflow.WorkflowPreDeleteListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Cleans up all agent eval data (tests, judges, runs, results, transcript files) when a workflow is deleted. Tests
 * cascade to scenarios, runs, results, and verdicts via database foreign keys. Transcript files are cleaned up by
 * {@link AgentEvalResultBeforeDeleteEventListener}.
 *
 * @author Ivica Cardic
 */
@Component
public class AgentEvalWorkflowPreDeleteListener implements WorkflowPreDeleteListener {

    private static final Logger logger = LoggerFactory.getLogger(AgentEvalWorkflowPreDeleteListener.class);

    private final AgentEvalTestService agentEvalTestService;
    private final AgentJudgeService agentJudgeService;

    AgentEvalWorkflowPreDeleteListener(
        AgentEvalTestService agentEvalTestService, AgentJudgeService agentJudgeService) {

        this.agentEvalTestService = agentEvalTestService;
        this.agentJudgeService = agentJudgeService;
    }

    @Override
    public void onWorkflowPreDelete(String workflowId) {
        logger.debug("Cleaning up agent eval data for workflow {}", workflowId);

        agentEvalTestService.deleteAgentEvalTestsByWorkflowId(workflowId);
        agentJudgeService.deleteAgentJudgesByWorkflowId(workflowId);
    }
}
