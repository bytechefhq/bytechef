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

/**
 * @author Ivica Cardic
 */
public class GuardrailsConstants {

    public static final String GUARDRAILS = "guardrails";

    // Operation modes
    public static final String MODE = "mode";
    public static final String MODE_CLASSIFY = "classify";
    public static final String MODE_SANITIZE = "sanitize";

    // Configuration
    public static final String SENSITIVE_WORDS = "sensitiveWords";
    public static final String PII_PATTERNS = "piiPatterns";
    public static final String CUSTOM_REGEX_PATTERNS = "customRegexPatterns";

    // Validation targets
    public static final String VALIDATE_INPUT = "validateInput";
    public static final String VALIDATE_OUTPUT = "validateOutput";

    // Response messages
    public static final String BLOCKED_MESSAGE = "blockedMessage";
    public static final String DEFAULT_BLOCKED_MESSAGE = "I cannot process this request due to content policy.";

    private GuardrailsConstants() {
    }
}
