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
 * Signals that the LLM returned a response the structured-output converter could not parse (schema drift, malformed
 * JSON, missing required fields). Distinct from a call failure (network/auth/quota) because the root cause is usually a
 * prompt or schema bug on our side rather than an upstream outage — operators should investigate the prompt first.
 *
 * <p>
 * Direct permit of {@link GuardrailException} (no longer extends {@link GuardrailUnavailableException}) so
 * pattern-matching {@code switch} over the sealed hierarchy is exhaustive.
 *
 * @author Ivica Cardic
 */
public final class GuardrailOutputParseException extends GuardrailException {

    private final String guardrailName;

    public GuardrailOutputParseException(String guardrailName, String reason, Throwable cause) {
        super(guardrailName + " guardrail could not complete: " + reason, cause);

        this.guardrailName = guardrailName;
    }

    @Override
    public String guardrailName() {
        return guardrailName;
    }

    @Override
    public GuardrailExceptionKind kind() {
        return GuardrailExceptionKind.OUTPUT_PARSE;
    }
}
