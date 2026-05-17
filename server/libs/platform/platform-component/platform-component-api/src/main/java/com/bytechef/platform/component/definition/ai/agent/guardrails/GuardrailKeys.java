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

package com.bytechef.platform.component.definition.ai.agent.guardrails;

/**
 * Stable component-key identifiers for every guardrail cluster element.
 *
 * @author Ivica Cardic
 */
public final class GuardrailKeys {

    public static final String JAILBREAK = "jailbreak";
    public static final String NSFW = "nsfw";
    public static final String TOPICAL_ALIGNMENT = "topicalAlignment";
    public static final String CUSTOM = "custom";
    public static final String LLM_PII = "llmPii";

    public static final String KEYWORDS = "keywords";
    public static final String PII = "pii";
    public static final String SECRET_KEYS = "secretKeys";
    public static final String URLS = "urls";
    public static final String CUSTOM_REGEX = "customRegex";

    private GuardrailKeys() {
    }
}
