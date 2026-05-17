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
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.SYSTEM_MESSAGE;

import com.bytechef.component.definition.Parameters;

/**
 * @author Ivica Cardic
 */
public final class ParametersUtils {

    private ParametersUtils() {
    }

    public static String resolveSystemMessage(Parameters parameters) {
        if (parameters == null) {
            return DEFAULT_SYSTEM_MESSAGE;
        }

        return parameters.getBoolean(CUSTOMIZE_SYSTEM_MESSAGE, false)
            ? parameters.getString(SYSTEM_MESSAGE, DEFAULT_SYSTEM_MESSAGE)
            : DEFAULT_SYSTEM_MESSAGE;
    }
}
