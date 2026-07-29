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

package com.bytechef.platform.component.facade;

import com.bytechef.platform.domain.OutputResponse;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Resolves the declared STATIC output of an action or trigger for a dry-run: the {@code sampleOutput}, falling back to
 * the {@code placeholder}, falling back to an empty {@link Map}. Never invokes a dynamic output function - the caller
 * is responsible for only passing an {@link OutputResponse} obtained from a static source.
 *
 * @author Ivica Cardic
 */
public final class DryRunOutputResolver {

    private DryRunOutputResolver() {
    }

    public static Object resolve(@Nullable OutputResponse outputResponse) {
        if (outputResponse == null) {
            return Map.of();
        }

        if (outputResponse.sampleOutput() != null) {
            return outputResponse.sampleOutput();
        }

        return outputResponse.placeholder() != null ? outputResponse.placeholder() : Map.of();
    }
}
