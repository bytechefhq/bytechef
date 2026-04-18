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
 * Mixin implemented by PREFLIGHT-stage guardrails that participate in text masking. Separating masking out of
 * {@link StagedGuardrail} makes the invariant compile-time-enforceable: LLM-stage guardrails do not implement this
 * mixin, so they cannot declare masking methods the advisor would silently ignore.
 *
 * <p>
 * Guardrails that declare {@link PreflightCheckFunction} or {@link PreflightSanitizerFunction} automatically inherit
 * this mixin. Direct implementations of {@link GuardrailCheckFunction} / {@link GuardrailSanitizerFunction} without
 * this mixin cannot mask — the advisor gates every masking call behind {@code instanceof PreflightMasking}.
 *
 * <p>
 * Implementations return a {@link MaskResult} that the advisor exhaustively switches on:
 * <ul>
 * <li>{@link MaskResult.Entities} — per-type entity map; merged and applied length-sorted across every preflight
 * guardrail so longer matches always win over shorter substrings.</li>
 * <li>{@link MaskResult.Masked} — the guardrail has already rewritten the text; the advisor uses it as the next stage's
 * input.</li>
 * <li>{@link MaskResult.Unchanged} — no masking; the advisor forwards the previous intermediate text unchanged.</li>
 * </ul>
 *
 * <p>
 * The closed-set return type replaces the earlier three-method dispatch ({@code preflightMaskEntities} +
 * {@code preflightMask} + {@code usesMaskEntityMap}) so it is impossible to silently short-circuit one mode by
 * populating another — the switch is exhaustive at compile time.
 *
 * @author Ivica Cardic
 */
public interface PreflightMasking {

    /**
     * Preflight-stage mask call. Default returns {@link MaskResult#unchanged()} so guardrails that inherit this mixin
     * via {@link PreflightCheckFunction} / {@link PreflightSanitizerFunction} without overriding it cost the advisor
     * nothing.
     */
    default MaskResult mask(String text, GuardrailContext context) {
        return MaskResult.unchanged();
    }
}
