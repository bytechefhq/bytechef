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

package com.bytechef.ai.copilot.agent;

import com.agui.core.agent.RunAgentInput;
import com.agui.core.exception.AGUIException;
import com.agui.spring.ai.SpringAIAgent;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

/**
 * @author Ivica Cardic
 */
public abstract class CopilotSpringAIAgent extends SpringAIAgent {

    private static final Logger log = LoggerFactory.getLogger(CopilotSpringAIAgent.class);

    private final @Nullable OverrideChatClientResolver overrideChatClientResolver;

    protected CopilotSpringAIAgent(
        SpringAIAgent.Builder builder, @Nullable OverrideChatClientResolver overrideChatClientResolver)
        throws AGUIException {

        super(builder);

        this.overrideChatClientResolver = overrideChatClientResolver;
    }

    @Override
    protected ChatClient resolveChatClient(RunAgentInput input) {
        if (overrideChatClientResolver == null) {
            return super.resolveChatClient(input);
        }

        try {
            ChatClient override = overrideChatClientResolver.resolve(input.state());

            if (override != null) {
                return override;
            }
        } catch (RuntimeException exception) {
            log.warn(
                "{}: override ChatClient resolver threw; falling back to default.", getClass().getSimpleName(),
                exception);
        }

        return super.resolveChatClient(input);
    }
}
