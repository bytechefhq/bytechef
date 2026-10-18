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

package com.bytechef.component.ai.typesafe.cluster;

import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.ACTION_THRESHOLD;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.CLUSTER_ELEMENT_MODEL_PROPERTY;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.REVIEW_THRESHOLD;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.SEVERITY_BLOCK_THRESHOLD;
import static com.bytechef.component.definition.ComponentDsl.number;

import com.bytechef.component.ai.typesafe.util.TypeSafeClientUtils;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailCheckFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailContext;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailStage;
import com.bytechef.platform.component.definition.ai.agent.guardrails.Violation;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springaicommunity.typesafe.TypeSafeClient;
import org.springaicommunity.typesafe.advisor.JevGuardrail;
import org.springaicommunity.typesafe.advisor.JevGuardrail.Hazard;
import org.springaicommunity.typesafe.advisor.JevGuardrail.Outcome;
import org.springaicommunity.typesafe.advisor.JevGuardrail.Verdict;

/**
 * @author Ivica Cardic
 */
public final class TypeSafeGuardrail {

    private static final double DEFAULT_REVIEW_THRESHOLD = 0.35;
    private static final double DEFAULT_ACTION_THRESHOLD = 0.7;
    private static final double DEFAULT_SEVERITY_BLOCK_THRESHOLD = 2.0;

    public static final ClusterElementDefinition<GuardrailCheckFunction> INPUT_CLUSTER_ELEMENT_DEFINITION = of(
        "jevInputGuardrail", "Jev Input Guardrail",
        "Screens the user's message with Jev before it reaches the model, for jailbreak and prompt-injection " +
            "attempts, requests to cause physical harm or commit a crime, and signs of self-harm. Needs no chat model.",
        true);

    public static final ClusterElementDefinition<GuardrailCheckFunction> OUTPUT_CLUSTER_ELEMENT_DEFINITION = of(
        "jevOutputGuardrail", "Jev Output Guardrail",
        "Screens the model's answer with Jev before it is returned, for help the model should have refused, help " +
            "causing physical harm or committing a crime, and content about self-harm. Needs no chat model.",
        false);

    private TypeSafeGuardrail() {
    }

    private static ClusterElementDefinition<GuardrailCheckFunction> of(
        String name, String title, String description, boolean input) {

        return ComponentDsl.<GuardrailCheckFunction>clusterElement(name)
            .title(title)
            .description(description)
            .type(GuardrailCheckFunction.CHECK_FOR_VIOLATIONS)
            .properties(
                CLUSTER_ELEMENT_MODEL_PROPERTY,
                number(ACTION_THRESHOLD)
                    .label("Action Threshold")
                    .description(
                        "A hazard whose value is above this blocks the text. Self-harm is flagged the same way.")
                    .defaultValue(DEFAULT_ACTION_THRESHOLD)
                    .minValue(0)
                    .maxValue(1)
                    .advancedOption(true),
                number(REVIEW_THRESHOLD)
                    .label("Review Threshold")
                    .description(
                        "A hazard whose value is between this and the action threshold lets the text through for " +
                            "review rather than blocking it. A value above the action threshold is treated as " +
                            "equal to it.")
                    .defaultValue(DEFAULT_REVIEW_THRESHOLD)
                    .minValue(0)
                    .maxValue(1)
                    .advancedOption(true),
                number(SEVERITY_BLOCK_THRESHOLD)
                    .label("Severity Block Threshold")
                    .description(
                        "A text left for review is blocked anyway when its severity, scored from 0 to 3, is at or " +
                            "above this.")
                    .defaultValue(DEFAULT_SEVERITY_BLOCK_THRESHOLD)
                    .minValue(0)
                    .maxValue(3)
                    .advancedOption(true))
            .object(() -> new GuardrailCheckFunction() {

                @Override
                public Optional<Violation> apply(String text, GuardrailContext context) {
                    Parameters inputParameters = context.inputParameters();

                    return screen(
                        name, input, inputParameters, text,
                        TypeSafeClientUtils.createTypeSafeClient(inputParameters, context.connectionParameters()));
                }

                @Override
                public GuardrailStage stage() {
                    return GuardrailStage.LLM;
                }

                @Override
                public boolean validatesInputByDefault() {
                    return input;
                }

                @Override
                public boolean validatesOutputByDefault() {
                    return !input;
                }

                @Override
                public boolean validatesStreamedOutput() {
                    return true;
                }
            });
    }

    static Optional<Violation> screen(
        String name, boolean input, Parameters inputParameters, String text, TypeSafeClient typeSafeClient) {

        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        JevGuardrail jevGuardrail = createGuardrail(input, inputParameters);

        Verdict verdict = jevGuardrail.screen(typeSafeClient, text);

        Outcome outcome = verdict.outcome();

        if (outcome != Outcome.BLOCK && outcome != Outcome.SUPPORT) {
            return Optional.empty();
        }

        List<String> hazards = verdict.triggered();

        if (hazards.isEmpty()) {
            hazards = verdict.flagged();
        }

        Map<String, Double> scores = verdict.scores();

        double confidenceScore = 0;

        for (String hazard : hazards) {
            confidenceScore = Math.max(confidenceScore, scores.getOrDefault(hazard, 0.0));
        }

        Map<String, Serializable> info = new LinkedHashMap<>();

        info.put("outcome", outcome.name());
        info.put("triggered", String.join(",", hazards));
        info.put("severity", verdict.severity());

        return Optional.of(Violation.ofClassification(name, Math.min(1.0, confidenceScore), info));
    }

    private static JevGuardrail createGuardrail(boolean input, Parameters inputParameters) {
        JevGuardrail defaultGuardrail = JevGuardrail.defaultOutputBattery();

        if (input) {
            defaultGuardrail = JevGuardrail.defaultInputBattery();
        }

        double actionThreshold = inputParameters.getDouble(ACTION_THRESHOLD, DEFAULT_ACTION_THRESHOLD);
        double reviewThreshold = inputParameters.getDouble(REVIEW_THRESHOLD, DEFAULT_REVIEW_THRESHOLD);

        JevGuardrail.Builder builder = JevGuardrail.builder(defaultGuardrail.name())
            .actionThreshold(actionThreshold)
            .reviewThreshold(Math.min(reviewThreshold, actionThreshold))
            .severityBlockThreshold(
                inputParameters.getDouble(SEVERITY_BLOCK_THRESHOLD, DEFAULT_SEVERITY_BLOCK_THRESHOLD));

        Map<String, Hazard> hazards = defaultGuardrail.hazards();

        for (Map.Entry<String, Hazard> entry : hazards.entrySet()) {
            builder.hazard(entry.getKey(), entry.getValue());
        }

        return builder.build();
    }
}
