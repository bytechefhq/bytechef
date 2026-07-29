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

package com.bytechef.platform.ratelimit;

/**
 * A sustained-rate + burst policy (Sim model: burst capacity = sustained rate x burst multiplier, token bucket).
 *
 * @author Ivica Cardic
 */
public record RateLimitPolicy(int permitsPerMinute, int burstMultiplier) {

    public RateLimitPolicy {
        if (permitsPerMinute < 1) {
            throw new IllegalArgumentException("permitsPerMinute must be >= 1");
        }

        if (burstMultiplier < 1) {
            throw new IllegalArgumentException("burstMultiplier must be >= 1");
        }
    }

    public long capacity() {
        return (long) permitsPerMinute * burstMultiplier;
    }
}
