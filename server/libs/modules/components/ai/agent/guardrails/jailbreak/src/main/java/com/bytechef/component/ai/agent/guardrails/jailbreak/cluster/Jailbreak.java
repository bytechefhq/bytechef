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

package com.bytechef.component.ai.agent.guardrails.jailbreak.cluster;

import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.CUSTOMIZE_PROMPT;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.DEFAULT_JAILBREAK_PROMPT;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.DEFAULT_THRESHOLD;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.PROMPT;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.THRESHOLD;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.VALIDATE_INPUT_PROPERTY;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.VALIDATE_OUTPUT_PROPERTY;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.agent.guardrails.MissingModelChildException;
import com.bytechef.component.ai.agent.guardrails.util.LlmClassifierUtils;
import com.bytechef.component.ai.agent.guardrails.util.LlmClassifierUtils.Verdict;
import com.bytechef.component.ai.agent.guardrails.util.ParametersUtils;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailCheckFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailContext;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailStage;
import com.bytechef.platform.component.definition.ai.agent.guardrails.Violation;
import java.util.List;
import java.util.Optional;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;

/**
 * LLM-based jailbreak / prompt-injection detection. Classifies whether the user input is attempting to subvert the
 * agent's instructions (role-override, system-prompt reveal, instruction smuggling). Runs at the {@code LLM} stage
 * after preflight masking.
 *
 * @author Ivica Cardic
 */
public final class Jailbreak {

    private Jailbreak() {
    }

    public static ClusterElementDefinition<GuardrailCheckFunction> of() {
        return ComponentDsl.<GuardrailCheckFunction>clusterElement("jailbreak")
            .title("Jailbreak")
            .description("LLM-based detection of jailbreak / prompt-injection attempts.")
            .type(GuardrailCheckFunction.CHECK_FOR_VIOLATIONS)
            .properties(
                VALIDATE_INPUT_PROPERTY,
                VALIDATE_OUTPUT_PROPERTY,
                bool(CUSTOMIZE_PROMPT)
                    .label("Customize Prompt")
                    .description(
                        "If false, uses the built-in jailbreak / prompt-injection classifier prompt.")
                    .defaultValue(false),
                string(PROMPT)
                    .label("Prompt")
                    .description("Classification instructions for the LLM.")
                    .defaultValue(DEFAULT_JAILBREAK_PROMPT)
                    .displayCondition(CUSTOMIZE_PROMPT + " == true"),
                number(THRESHOLD)
                    .label("Threshold")
                    .description("Minimum confidence score required to flag as violation (0.0-1.0).")
                    .defaultValue(DEFAULT_THRESHOLD)
                    .minValue(0)
                    .maxValue(1))
            .object(() -> new GuardrailCheckFunction() {

                @Override
                public Optional<Violation> apply(String text, GuardrailContext context) throws Exception {
                    return Jailbreak.apply(text, context);
                }

                @Override
                public GuardrailStage stage() {
                    return GuardrailStage.LLM;
                }

                @Override
                public boolean requiresChatClient() {
                    return true;
                }
            });
    }

    private static Optional<Violation> apply(String text, GuardrailContext context) throws Exception {
        ChatClient chatClient = context.requireChatClient(() -> new MissingModelChildException("jailbreak"));

        Parameters inputParameters = context.inputParameters();

        String userPrompt = inputParameters.getBoolean(CUSTOMIZE_PROMPT, false)
            ? inputParameters.getString(PROMPT, DEFAULT_JAILBREAK_PROMPT)
            : DEFAULT_JAILBREAK_PROMPT;

        String systemMessage = ParametersUtils.resolveSystemMessage(context.parentParameters());

        double threshold = inputParameters.getDouble(THRESHOLD, DEFAULT_THRESHOLD);

        return classifyWith(chatClient, userPrompt, threshold, systemMessage, text, context.conversationHistory());
    }

    static Optional<Violation> classifyWith(
        ChatClient chatClient, String userPrompt, double threshold, String systemMessage, String text) {

        return classifyWith(chatClient, userPrompt, threshold, systemMessage, text, List.of());
    }

    static Optional<Violation> classifyWith(
        ChatClient chatClient, String userPrompt, double threshold, String systemMessage, String text,
        List<Message> conversationHistory) {

        Verdict verdict = LlmClassifierUtils.classify(
            "jailbreak", chatClient, systemMessage, userPrompt, text, threshold, conversationHistory);

        if (!verdict.violated()) {
            return Optional.empty();
        }

        return Optional.of(Violation.ofClassification("jailbreak", verdict.confidenceScore()));
    }
}
