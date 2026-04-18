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
 * Pipeline stage for guardrail check and sanitizer functions. Rule-based (regex/keyword/URL/secret) guardrails run in
 * {@link #PREFLIGHT} so their masks are visible to {@link #LLM}-stage classifiers that see the masked text. Shared
 * between {@link GuardrailCheckFunction} and {@link GuardrailSanitizerFunction} so the two-stage pipeline is expressed
 * once.
 *
 * @author Ivica Cardic
 */
public enum GuardrailStage {
    PREFLIGHT, LLM
}
