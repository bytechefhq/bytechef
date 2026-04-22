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

package com.bytechef.ee.platform.ai.llm.usage;

/**
 * Discriminator stamped onto every {@link AiLlmUsage} row so analytics queries can split spend by where the call
 * originated. Persisted as the enum's ordinal — APPEND-ONLY: new sources MUST be added at the end so existing rows keep
 * their meaning.
 *
 * @author Ivica Cardic
 */
public enum LlmUsageSource {

    /** AI Hub agents (build / ask routing agents and their subagent calls). */
    AI_HUB,

    /** AI Gateway routed requests (workspace-scoped LLM proxy). */
    AI_GATEWAY,

    /** Standalone AI agent runtime (skill / agent invocations outside AI Hub). */
    AI_AGENT,

    /** Catch-all for ad-hoc invocations not bound to any specific surface. */
    OTHER
}
