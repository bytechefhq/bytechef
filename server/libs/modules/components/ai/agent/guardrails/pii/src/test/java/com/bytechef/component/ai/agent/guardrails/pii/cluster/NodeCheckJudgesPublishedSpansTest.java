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

package com.bytechef.component.ai.agent.guardrails.pii.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.component.ai.agent.guardrails.advisor.CheckForViolationsAdvisor;
import com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants;
import com.bytechef.component.definition.Context;
import com.bytechef.platform.ai.guardrails.GuardrailAdvisorOrder;
import com.bytechef.platform.ai.guardrails.PublishedInputSpans;
import com.bytechef.platform.ai.sensitivedata.SensitiveKind;
import com.bytechef.platform.ai.sensitivedata.SensitiveSpan;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class NodeCheckJudgesPublishedSpansTest {

    private static final String EMAIL = "bob@acme.io";
    private static final String TOKEN = "[PII_EMAIL_ADDRESS_1_abcd]";

    @Test
    void testANodeBlockOnInputPiiStillFiresWhenTheFloorTokenizedIt() {
        ChatClientResponse chatClientResponse = ChatClient.create(echoModel(TOKEN))
            .prompt("please mail " + EMAIL)
            .advisors(floorStandIn(true))
            .advisors(nodePiiCheck(true, false))
            .call()
            .chatClientResponse();

        ChatResponse chatResponse = Objects.requireNonNull(chatClientResponse.chatResponse(), "chatResponse");
        Generation result = Objects.requireNonNull(chatResponse.getResult(), "result");
        AssistantMessage output = Objects.requireNonNull(result.getOutput(), "output");
        String content = Objects.requireNonNull(output.getText(), "text");

        assertThat(content)
            .as("the node judged the floor's published detection, not the tokens it received")
            .isEqualTo("blocked");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> violations = (List<Map<String, Object>>) chatResponse.getMetadata()
            .get(GuardrailsConstants.VIOLATIONS_METADATA_KEY);

        assertThat(violations)
            .as("the block is a real policy verdict over published spans, not a fail-closed execution failure")
            .hasSize(1);
        assertThat(violations.getFirst())
            .as("distinguishes a piiCheck policy block from any other guardrail or from a fail-closed block")
            .containsEntry("guardrail", "piiCheck")
            .containsEntry("executionFailed", false);
    }

    @Test
    void testWithoutPublicationTheSameNodeCheckIsBlindNegativeControl() {
        String content = ChatClient.create(echoModel(TOKEN))
            .prompt("please mail " + EMAIL)
            .advisors(floorStandIn(false))
            .advisors(nodePiiCheck(true, false))
            .call()
            .content();

        assertThat(content)
            .as("proves the verdict above came from publication: same floor, same node, no spans -> no block")
            .isEqualTo(EMAIL);
    }

    @Test
    void testANodeOutputCheckDoesNotFireOnTheCallersRestoredValue() {
        String content = ChatClient.create(echoModel(TOKEN))
            .prompt("please mail " + EMAIL)
            .advisors(floorStandIn(true))
            .advisors(nodePiiCheck(false, true))
            .call()
            .content();

        assertThat(content)
            .as("the node saw the token on the response; the floor restored the caller's value afterwards")
            .isEqualTo(EMAIL);
    }

    private static CheckForViolationsAdvisor nodePiiCheck(boolean validateInput, boolean validateOutput) {
        return CheckForViolationsAdvisor.builder()
            .context(mock(Context.class))
            .blockedMessage("blocked")
            .add(
                "piiCheck", Pii.ofCheck()
                    .getElement(),
                ParametersFactory.create(
                    Map.of("type", "ALL", "validateInput", validateInput, "validateOutput", validateOutput)),
                ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of()))
            .build();
    }

    private static CallAdvisor floorStandIn(boolean publish) {
        return new CallAdvisor() {

            @Override
            public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
                List<Message> patched = new ArrayList<>();
                int start = -1;

                for (Message message : request.prompt()
                    .getInstructions()) {
                    String text = message.getText();

                    if (text != null && text.contains(EMAIL)) {
                        start = text.indexOf(EMAIL);

                        patched.add(new UserMessage(text.replace(EMAIL, TOKEN)));
                    } else {
                        patched.add(message);
                    }
                }

                ChatClientRequest.Builder builder = request.mutate()
                    .prompt(new Prompt(patched, request.prompt()
                        .getOptions()));

                if (publish && start >= 0) {
                    builder.context(
                        PublishedInputSpans.CONTEXT_KEY,
                        List.of(SensitiveSpan.of(SensitiveKind.PII, "EMAIL_ADDRESS", start, start + EMAIL.length())));
                }

                ChatClientResponse response = chain.nextCall(builder.build());
                ChatResponse chatResponse = Objects.requireNonNull(response.chatResponse(), "chatResponse");
                Generation result = Objects.requireNonNull(chatResponse.getResult(), "result");
                AssistantMessage output = Objects.requireNonNull(result.getOutput(), "output");
                String text = Objects.requireNonNull(output.getText(), "text");
                String restored = text.replace(TOKEN, EMAIL);

                return ChatClientResponse.builder()
                    .chatResponse(
                        new ChatResponse(
                            List.of(new Generation(new AssistantMessage(restored))), chatResponse.getMetadata()))
                    .context(response.context())
                    .build();
            }

            @Override
            public String getName() {
                return "floor-stand-in";
            }

            @Override
            public int getOrder() {
                return GuardrailAdvisorOrder.WORKSPACE_FLOOR;
            }
        };
    }

    private static ChatModel echoModel(String reply) {
        return new ChatModel() {

            @Override
            public ChatResponse call(Prompt prompt) {
                return new ChatResponse(List.of(new Generation(new AssistantMessage(reply))));
            }
        };
    }
}
