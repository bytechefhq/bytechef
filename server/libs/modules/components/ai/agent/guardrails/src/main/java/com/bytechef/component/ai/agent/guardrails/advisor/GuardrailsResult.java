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

import java.util.Map;

/**
 * Result of a guardrail check.
 *
 * @author Ivica Cardic
 */
public record GuardrailsResult(
    String guardrailName,
    boolean tripwireTriggered,
    double confidenceScore,
    Map<String, Object> info) {

    public GuardrailsResult {
        info = info == null ? Map.of() : Map.copyOf(info);
    }

    /**
     * Create a passed result indicating no violation.
     *
     * @param guardrailName the name of the guardrail
     * @return a passed result
     */
    public static GuardrailsResult passed(String guardrailName) {
        return new GuardrailsResult(guardrailName, false, 0.0, Map.of());
    }

    /**
     * Create a blocked result indicating a violation.
     *
     * @param guardrailName   the name of the guardrail
     * @param confidenceScore the confidence score (0.0 to 1.0)
     * @param info            additional information about the violation
     * @return a blocked result
     */
    public static GuardrailsResult blocked(String guardrailName, double confidenceScore, Map<String, Object> info) {
        return new GuardrailsResult(guardrailName, true, confidenceScore, info);
    }
}
