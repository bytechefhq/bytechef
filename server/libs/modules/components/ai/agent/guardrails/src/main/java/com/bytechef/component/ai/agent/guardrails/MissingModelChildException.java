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

package com.bytechef.component.ai.agent.guardrails;

/**
 * Signals that an LLM-based guardrail was invoked without a MODEL child cluster element attached. Thrown so the
 * {@code CheckForViolationsAdvisor}'s fail-closed catch block treats the misconfiguration as a blocking violation
 * rather than silently passing every check.
 *
 * @author Ivica Cardic
 */
public final class MissingModelChildException extends GuardrailException {

    private final String guardrailName;

    public MissingModelChildException(String guardrailName) {
        super(guardrailName + " guardrail has no MODEL child cluster element configured. Attach a chat model to " +
            "enable detection, or remove the guardrail.");

        this.guardrailName = guardrailName;
    }

    @Override
    public String guardrailName() {
        return guardrailName;
    }

    @Override
    public GuardrailExceptionKind kind() {
        return GuardrailExceptionKind.MISSING_MODEL;
    }
}
