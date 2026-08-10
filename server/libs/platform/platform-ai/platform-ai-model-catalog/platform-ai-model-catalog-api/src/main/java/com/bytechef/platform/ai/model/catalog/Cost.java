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

package com.bytechef.platform.ai.model.catalog;

import java.math.BigDecimal;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Per-million-token rates in USD.
 *
 * <p>
 * {@code tiers} carries context-size-dependent pricing (23 of the 249 mapped models charge more above a context
 * threshold). The AI gateway's two flat cost columns cannot express this and use the base {@code input} / {@code
 * output} rates; the tiers are modeled so a future gateway change is a consumer change rather than a re-parse.
 *
 * @author Ivica Cardic
 */
public record Cost(
    @Nullable BigDecimal input, @Nullable BigDecimal output, @Nullable BigDecimal cacheRead,
    @Nullable BigDecimal cacheWrite, @Nullable BigDecimal reasoning, @Nullable BigDecimal inputAudio,
    @Nullable BigDecimal outputAudio, List<CostTier> tiers) {

    /**
     * Defensively copies {@code tiers} so the record cannot be used to smuggle a caller-held mutable list into its
     * internal state or hand one back out. {@link List#copyOf} preserves the tiers' encounter order, which matters
     * since {@code contextSize} thresholds are meant to be read ascending.
     */
    public Cost {
        tiers = List.copyOf(tiers);
    }
}
