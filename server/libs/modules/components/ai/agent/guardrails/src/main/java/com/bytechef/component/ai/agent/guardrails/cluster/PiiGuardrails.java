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
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.PII_PATTERNS;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.VALIDATE_INPUT;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.VALIDATE_OUTPUT;
import static com.bytechef.platform.component.definition.ai.agent.GuardrailsFunction.GUARDRAILS;

import com.bytechef.component.ai.agent.guardrails.advisor.GuardrailsAdvisor;
import com.bytechef.component.ai.agent.guardrails.util.PiiDetector;
import com.bytechef.component.ai.agent.guardrails.util.PiiDetector.PiiPattern;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ai.agent.GuardrailsFunction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.ai.chat.client.advisor.api.Advisor;

/**
 * PII detection guardrails cluster element.
 *
 * @author Ivica Cardic
 */
public class PiiGuardrails {

    public static ClusterElementDefinition<GuardrailsFunction> of() {
        return new PiiGuardrails().build();
    }

    private PiiGuardrails() {
    }

    private ClusterElementDefinition<GuardrailsFunction> build() {
        return ComponentDsl.<GuardrailsFunction>clusterElement("piiGuardrails")
            .title("PII Guardrails")
            .description(
                "Detect and protect personally identifiable information (PII) such as email addresses, phone " +
                    "numbers, social security numbers, and credit card numbers.")
            .type(GUARDRAILS)
            .properties(
                ComponentDsl.string(MODE)
                    .label("Mode")
                    .description("Operation mode: classify (block) or sanitize (mask).")
                    .options(
                        ComponentDsl.option("Classify (Block)", MODE_CLASSIFY),
                        ComponentDsl.option("Sanitize (Mask)", MODE_SANITIZE))
                    .defaultValue(MODE_SANITIZE),
                ComponentDsl.array(PII_PATTERNS)
                    .label("Additional PII Patterns")
                    .description("Custom regex patterns for PII detection (optional). Default patterns detect email, " +
                        "phone, SSN, credit cards, and IP addresses.")
                    .items(ComponentDsl.string())
                    .required(false),
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
                    .description("Message to return when content is blocked (only used in classify mode).")
                    .defaultValue(DEFAULT_BLOCKED_MESSAGE))
            .object(() -> this::apply);
    }

    protected Advisor apply(
        Parameters inputParameters, Parameters connectionParameters, Parameters extensions,
        Map<String, ComponentConnection> componentConnections) {

        List<PiiPattern> patterns = new ArrayList<>(PiiDetector.DEFAULT_PII_PATTERNS);

        // Add custom patterns if provided
        List<String> customPatternStrings = inputParameters.getList(PII_PATTERNS, String.class);

        if (customPatternStrings != null) {
            for (String patternString : customPatternStrings) {
                if (patternString != null && !patternString.isEmpty()) {
                    patterns.add(new PiiPattern("CUSTOM", Pattern.compile(patternString)));
                }
            }
        }

        return GuardrailsAdvisor.builder()
            .piiPatterns(patterns)
            .mode(inputParameters.getString(MODE, MODE_SANITIZE))
            .validateInput(inputParameters.getBoolean(VALIDATE_INPUT, true))
            .validateOutput(inputParameters.getBoolean(VALIDATE_OUTPUT, true))
            .blockedMessage(inputParameters.getString(BLOCKED_MESSAGE, DEFAULT_BLOCKED_MESSAGE))
            .build();
    }
}
