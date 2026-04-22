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
 * Append-only write-side service for AI LLM-call usage tracking. One row per LLM completion across every agent surface
 * (AI Hub, AI Gateway, ad-hoc AI agents) — the {@link LlmUsageContext#source()} discriminates which surface issued the
 * call so analytics can split spend by origin.
 *
 * <p>
 * Implementations are responsible for their own persistence + cost estimation; the interface only carries the call-site
 * facts the recorder needs. Implementations must never throw — recording failures should be logged and swallowed so a
 * bookkeeping problem cannot break the end-user request.
 * </p>
 *
 * @author Ivica Cardic
 */
public interface LlmUsageRecorder {

    /**
     * Records a single LLM completion.
     *
     * @param context      workspace + user + source + owner-context the call ran under; never {@code null}
     * @param model        canonical name of the model that served the call; never blank
     * @param inputTokens  prompt tokens consumed; clamped to {@code >= 0} by the recorder
     * @param outputTokens completion tokens produced; clamped to {@code >= 0} by the recorder
     * @param durationMs   wall-clock duration of the LLM call in milliseconds; clamped to {@code >= 0} by the recorder
     */
    void recordLlm(LlmUsageContext context, String model, int inputTokens, int outputTokens, long durationMs);
}
