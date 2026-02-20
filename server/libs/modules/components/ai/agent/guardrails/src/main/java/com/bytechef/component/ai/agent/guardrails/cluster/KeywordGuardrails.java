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

package com.bytechef.component.ai.agent.guardrails.cluster;

import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.BLOCKED_MESSAGE;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.DEFAULT_BLOCKED_MESSAGE;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.MODE;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.MODE_CLASSIFY;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.MODE_SANITIZE;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.SENSITIVE_WORDS;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.VALIDATE_INPUT;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.VALIDATE_OUTPUT;
import static com.bytechef.platform.component.definition.ai.agent.GuardrailsFunction.GUARDRAILS;

import com.bytechef.component.ai.agent.guardrails.advisor.GuardrailsAdvisor;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ai.agent.GuardrailsFunction;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.client.advisor.api.Advisor;

/**
 * Keyword-based guardrails cluster element.
 *
 * @author Ivica Cardic
 */
public class KeywordGuardrails {

    public static ClusterElementDefinition<GuardrailsFunction> of() {
        return new KeywordGuardrails().build();
    }

    private KeywordGuardrails() {
    }

    private ClusterElementDefinition<GuardrailsFunction> build() {
        return ComponentDsl.<GuardrailsFunction>clusterElement("keywordGuardrails")
            .title("Keyword Guardrails")
            .description("Block or sanitize content containing specified keywords or phrases.")
            .type(GUARDRAILS)
            .properties(
                ComponentDsl.string(MODE)
                    .label("Mode")
                    .description("Operation mode: classify (block) or sanitize (mask).")
                    .options(
                        ComponentDsl.option("Classify (Block)", MODE_CLASSIFY),
                        ComponentDsl.option("Sanitize (Mask)", MODE_SANITIZE))
                    .defaultValue(MODE_CLASSIFY),
                ComponentDsl.array(SENSITIVE_WORDS)
                    .label("Sensitive Words")
                    .description("List of words or phrases to detect.")
                    .items(ComponentDsl.string())
                    .required(true),
                ComponentDsl.bool(VALIDATE_INPUT)
                    .label("Validate Input")
                    .description("Check user input before sending to model.")
                    .defaultValue(true),
                ComponentDsl.bool(VALIDATE_OUTPUT)
                    .label("Validate Output")
                    .description("Check model response before returning.")
                    .defaultValue(true),
                ComponentDsl.string(BLOCKED_MESSAGE)
                    .label("Blocked Message")
                    .description("Message to return when content is blocked.")
                    .defaultValue(DEFAULT_BLOCKED_MESSAGE))
            .object(() -> this::apply);
    }

    protected Advisor apply(
        Parameters inputParameters, Parameters connectionParameters, Parameters extensions,
        Map<String, ComponentConnection> componentConnections) {

        List<String> sensitiveWords = inputParameters.getList(SENSITIVE_WORDS, String.class);

        return GuardrailsAdvisor.builder()
            .sensitiveWords(sensitiveWords)
            .mode(inputParameters.getString(MODE, MODE_CLASSIFY))
            .validateInput(inputParameters.getBoolean(VALIDATE_INPUT, true))
            .validateOutput(inputParameters.getBoolean(VALIDATE_OUTPUT, true))
            .blockedMessage(inputParameters.getString(BLOCKED_MESSAGE, DEFAULT_BLOCKED_MESSAGE))
            .build();
    }
}
