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

package com.bytechef.ai.copilot.service;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.AgentSubscriberParams;
import com.agui.core.agent.RunAgentParameters;
import com.agui.core.message.BaseMessage;
import com.agui.core.message.UserMessage;
import com.agui.core.state.State;
import com.agui.server.LocalAgent;
import com.bytechef.ai.copilot.constant.CopilotConstants;
import com.bytechef.ai.copilot.util.Mode;
import com.bytechef.ai.copilot.util.Source;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Synchronously drives the {@code skills_build} agent end-to-end so a caller can create a skill from a single prompt
 * after the agent has populated its SKILL.md via its tools.
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class CopilotSkillGeneratorImpl implements CopilotSkillGenerator {

    private static final Logger log = LoggerFactory.getLogger(CopilotSkillGeneratorImpl.class);

    private static final long DEFAULT_TIMEOUT_MINUTES = 10;

    private final Map<String, LocalAgent> localAgentMap;

    @SuppressFBWarnings("EI")
    public CopilotSkillGeneratorImpl(List<LocalAgent> localAgents) {
        this.localAgentMap = localAgents.stream()
            .collect(Collectors.toMap(LocalAgent::getAgentId, localAgent -> localAgent));
    }

    @Override
    public void generateSkill(long skillId, String prompt) {
        String agentId = (Source.SKILLS.name() + "_" + Mode.BUILD.name()).toLowerCase();

        LocalAgent localAgent = localAgentMap.get(agentId);

        if (localAgent == null) {
            throw new IllegalStateException("Skills BUILD agent not available: " + agentId);
        }

        Map<String, Object> stateMap = new HashMap<>();

        stateMap.put("currentSelectedSkillId", skillId);
        stateMap.put("mode", Mode.BUILD.name());
        stateMap.put("autonomous", true);

        stateMap.put(CopilotConstants.STATE_TENANT_ID, TenantContext.getCurrentTenantId());

        Authentication authentication = SecurityContextHolder.getContext()
            .getAuthentication();

        if (authentication != null) {
            stateMap.put(CopilotConstants.STATE_AUTHENTICATION, authentication);
        }

        State state = new State(stateMap);

        UserMessage userMessage = new UserMessage();

        UUID uuid = UUID.randomUUID();

        userMessage.setId(uuid.toString());
        userMessage.setContent(prompt);

        RunAgentParameters parameters = RunAgentParameters.builder()
            .threadId(uuid.toString())
            .runId(uuid.toString())
            .messages(List.<BaseMessage>of(userMessage))
            .state(state)
            .build();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        localAgent.runAgent(parameters, new AgentSubscriber() {

            @Override
            public void onRunFinalized(AgentSubscriberParams params) {
                latch.countDown();
            }

            @Override
            public void onRunFailed(AgentSubscriberParams params, Throwable throwable) {
                errorRef.set(throwable);

                latch.countDown();
            }
        });

        try {
            if (!latch.await(DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                throw new IllegalStateException(
                    "Skill generation timed out after " + DEFAULT_TIMEOUT_MINUTES + " minutes for skillId=" + skillId);
            }
        } catch (InterruptedException interruptedException) {
            Thread.currentThread()
                .interrupt();

            throw new IllegalStateException("Skill generation was interrupted for skillId=" + skillId,
                interruptedException);
        }

        Throwable error = errorRef.get();

        if (error != null) {
            log.error("Skill generation failed for skillId={}", skillId, error);

            throw new IllegalStateException("Skill generation failed: " + error.getMessage(), error);
        }
    }
}
