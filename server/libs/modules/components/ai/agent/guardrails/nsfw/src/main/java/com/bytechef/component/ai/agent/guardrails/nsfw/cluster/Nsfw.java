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

package com.bytechef.component.ai.agent.guardrails.nsfw.cluster;

import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.CUSTOMIZE_PROMPT;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.DEFAULT_NSFW_PROMPT;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.DEFAULT_THRESHOLD;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.PROMPT;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.THRESHOLD;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.agent.guardrails.MissingModelChildException;
import com.bytechef.component.ai.agent.guardrails.util.GuardrailProperties;
import com.bytechef.component.ai.agent.guardrails.util.LlmClassifier;
import com.bytechef.component.ai.agent.guardrails.util.LlmClassifier.Verdict;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailCheckFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailContext;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailStage;
import com.bytechef.platform.component.definition.ai.agent.guardrails.Violation;
import java.util.Optional;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component("nsfw_v1_ClusterElement")
public final class Nsfw {

    public ClusterElementDefinition<GuardrailCheckFunction> of() {
        return ComponentDsl.<GuardrailCheckFunction>clusterElement("nsfw")
            .title("NSFW")
            .description("LLM-based detection of Not Safe For Work content.")
            .type(GuardrailCheckFunction.CHECK_FOR_VIOLATIONS)
            .properties(
                bool(CUSTOMIZE_PROMPT)
                    .label("Customize Prompt")
                    .defaultValue(false),
                string(PROMPT)
                    .label("Prompt")
                    .defaultValue(DEFAULT_NSFW_PROMPT)
                    .displayCondition(CUSTOMIZE_PROMPT + " == true"),
                number(THRESHOLD)
                    .label("Threshold")
                    .description("Minimum confidence score (0.0-1.0) required to flag.")
                    .defaultValue(DEFAULT_THRESHOLD),
                GuardrailProperties.failMode())
            .object(() -> new GuardrailCheckFunction() {

                @Override
                public Optional<Violation> apply(String text, GuardrailContext context) throws Exception {
                    return Nsfw.this.apply(text, context);
                }

                @Override
                public GuardrailStage stage() {
                    return GuardrailStage.LLM;
                }
            });
    }

    private Optional<Violation> apply(String text, GuardrailContext context) throws Exception {
        // Fail-closed via exception: advisor converts this to a blocking violation.
        ChatClient chatClient = context.chatClient()
            .orElseThrow(() -> new MissingModelChildException("nsfw"));

        Parameters inputParameters = context.inputParameters();

        String userPrompt = inputParameters.getBoolean(CUSTOMIZE_PROMPT, false)
            ? inputParameters.getString(PROMPT, DEFAULT_NSFW_PROMPT)
            : DEFAULT_NSFW_PROMPT;

        String systemMessage = GuardrailProperties.resolveSystemMessage(context.parentParameters());

        double threshold = inputParameters.getDouble(THRESHOLD, DEFAULT_THRESHOLD);

        return classifyWith(chatClient, userPrompt, threshold, systemMessage, text);
    }

    /** Package-private seam for testing. */
    static Optional<Violation> classifyWith(
        ChatClient chatClient, String userPrompt, double threshold, String systemMessage, String text) {

        Verdict verdict = LlmClassifier.classify("nsfw", chatClient, systemMessage, userPrompt, text, threshold);

        if (!verdict.violated()) {
            return Optional.empty();
        }

        return Optional.of(Violation.ofClassification("nsfw", verdict.confidenceScore()));
    }
}
