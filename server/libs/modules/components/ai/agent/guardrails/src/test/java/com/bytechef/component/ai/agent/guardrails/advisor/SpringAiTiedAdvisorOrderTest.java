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

package com.bytechef.component.ai.agent.guardrails.advisor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.Ordered;

/**
 * @author Ivica Cardic
 */
class SpringAiTiedAdvisorOrderTest {

    @Test
    void testOnAnOrderTieTheLaterRegisteredAdvisorRunsFirst() {
        List<String> trace = new ArrayList<>();

        ChatClient.create(stubChatModel())
            .prompt("hello")
            .advisors(new TracingAdvisor("registered-first", Ordered.HIGHEST_PRECEDENCE, trace))
            .advisors(new TracingAdvisor("registered-second", Ordered.HIGHEST_PRECEDENCE, trace))
            .call()
            .content();

        assertThat(trace)
            .as("request direction runs outermost first; the later registration is outermost on a tie")
            .containsExactly("registered-second", "registered-first");
    }

    @Test
    void testADistinctOrderValueWinsRegardlessOfRegistration() {
        List<String> trace = new ArrayList<>();

        ChatClient.create(stubChatModel())
            .prompt("hello")
            .advisors(new TracingAdvisor("registered-first-lower-precedence", Ordered.HIGHEST_PRECEDENCE + 1, trace))
            .advisors(new TracingAdvisor("registered-second-highest", Ordered.HIGHEST_PRECEDENCE, trace))
            .call()
            .content();

        assertThat(trace).containsExactly("registered-second-highest", "registered-first-lower-precedence");

        trace.clear();

        ChatClient.create(stubChatModel())
            .prompt("hello")
            .advisors(new TracingAdvisor("registered-first-highest", Ordered.HIGHEST_PRECEDENCE, trace))
            .advisors(new TracingAdvisor("registered-second-lower-precedence", Ordered.HIGHEST_PRECEDENCE + 1, trace))
            .call()
            .content();

        assertThat(trace).containsExactly("registered-first-highest", "registered-second-lower-precedence");
    }

    private static ChatModel stubChatModel() {
        return new ChatModel() {

            @Override
            public ChatResponse call(Prompt prompt) {
                return new ChatResponse(List.of(new Generation(new AssistantMessage("ok"))));
            }
        };
    }

    private record TracingAdvisor(String name, int order, List<String> trace) implements CallAdvisor {

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            trace.add(name);

            return chain.nextCall(request);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getOrder() {
            return order;
        }
    }
}
