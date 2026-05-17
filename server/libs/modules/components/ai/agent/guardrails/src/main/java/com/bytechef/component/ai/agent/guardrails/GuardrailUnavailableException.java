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

import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public non-sealed class GuardrailUnavailableException extends GuardrailException {

    private final String guardrailName;

    public GuardrailUnavailableException(String guardrailName, String reason) {
        super(guardrailName + " guardrail could not complete: " + reason);

        this.guardrailName = guardrailName;
    }

    public GuardrailUnavailableException(String guardrailName, String reason, Throwable cause) {
        super(guardrailName + " guardrail could not complete: " + reason, cause);

        this.guardrailName = guardrailName;
    }

    @Override
    public Optional<String> guardrailName() {
        return Optional.of(guardrailName);
    }

    @Override
    public GuardrailExceptionKind kind() {
        return GuardrailExceptionKind.UPSTREAM_UNAVAILABLE;
    }
}
