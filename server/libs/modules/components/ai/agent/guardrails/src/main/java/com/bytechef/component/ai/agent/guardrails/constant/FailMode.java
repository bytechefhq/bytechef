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

package com.bytechef.component.ai.agent.guardrails.constant;

import org.slf4j.Logger;

/**
 * Typed fail-mode value set. The string constants in {@link GuardrailsConstants#FAIL_CLOSED} and
 * {@link GuardrailsConstants#FAIL_OPEN} remain for property-option wiring (the DSL takes string option values); runtime
 * comparisons should use this enum to get compile-time typo protection.
 *
 * @author Ivica Cardic
 */
public enum FailMode {

    FAIL_CLOSED,
    FAIL_OPEN;

    /**
     * Parse a raw string from the property layer into a {@link FailMode}. Unknown values are logged at WARN and default
     * to {@link #FAIL_CLOSED} — fail-safe when the operator misconfigures the property.
     */
    public static FailMode parse(String raw, Logger log) {
        if (raw == null) {
            return FAIL_CLOSED;
        }

        try {
            return FailMode.valueOf(raw);
        } catch (IllegalArgumentException exception) {
            log.warn("Unknown failMode '{}'; expected one of FAIL_CLOSED|FAIL_OPEN. Defaulting to FAIL_CLOSED.", raw);

            return FAIL_CLOSED;
        }
    }
}
