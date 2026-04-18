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

package com.bytechef.component.ai.agent.guardrails.util;

import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.CUSTOMIZE_SYSTEM_MESSAGE;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.DEFAULT_SYSTEM_MESSAGE;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.FAIL_CLOSED;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.FAIL_MODE;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.FAIL_OPEN;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.SYSTEM_MESSAGE;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.definition.Parameters;

/**
 * Shared property factories reused across every guardrail cluster element.
 *
 * @author Ivica Cardic
 */
public final class GuardrailProperties {

    private GuardrailProperties() {
    }

    /**
     * Optional per-check fail mode property. Defaults to {@code FAIL_CLOSED} — a check that cannot run (LLM outage,
     * missing MODEL, config error) blocks the request. Operators who prioritise availability can flip to
     * {@code FAIL_OPEN} per check to record the failure without blocking.
     */
    public static ModifiableStringProperty failMode() {
        return string(FAIL_MODE)
            .label("Fail Mode")
            .description(
                "What to do when this check cannot run. FAIL_CLOSED blocks the request (safer); " +
                    "FAIL_OPEN records the failure and lets the request through (availability-first).")
            .options(
                option("Fail closed (block)", FAIL_CLOSED),
                option("Fail open (allow)", FAIL_OPEN))
            .defaultValue(FAIL_CLOSED)
            .required(false);
    }

    /**
     * Resolve the effective system message from the parent (CheckForViolations) parameters. Falls back to
     * {@link com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants#DEFAULT_SYSTEM_MESSAGE} when
     * {@code parentParameters} is {@code null} (e.g. when an LLM guardrail cluster element is invoked without a
     * CheckForViolations parent — direct test construction or future sanitize-side reuse). Without this null guard the
     * four LLM cluster elements NPE inside apply().
     */
    public static String resolveSystemMessage(Parameters parentParameters) {
        if (parentParameters == null) {
            return DEFAULT_SYSTEM_MESSAGE;
        }

        return parentParameters.getBoolean(CUSTOMIZE_SYSTEM_MESSAGE, false)
            ? parentParameters.getString(SYSTEM_MESSAGE, DEFAULT_SYSTEM_MESSAGE)
            : DEFAULT_SYSTEM_MESSAGE;
    }
}
