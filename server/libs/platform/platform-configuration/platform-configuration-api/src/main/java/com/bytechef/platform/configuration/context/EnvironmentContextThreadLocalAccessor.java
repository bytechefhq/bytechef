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

package com.bytechef.platform.configuration.context;

import com.bytechef.platform.configuration.domain.Environment;
import io.micrometer.context.ThreadLocalAccessor;
import org.jspecify.annotations.Nullable;

/**
 * Bridges the thread-bound {@link EnvironmentContext} into the Micrometer {@code ContextRegistry} so Reactor's
 * automatic context propagation carries the environment across reactive thread hops (e.g. Spring AI advisor chains
 * scheduled on {@code Schedulers.boundedElastic()}, where {@code EmbeddingModel.embed()} / {@code ChatModel.stream()}
 * resolve the environment-scoped provider). {@link #getValue()} returns {@code null} when unset so the accessor never
 * propagates a spurious {@link Environment#PRODUCTION} fallback.
 *
 * @author Ivica Cardic
 */
public class EnvironmentContextThreadLocalAccessor implements ThreadLocalAccessor<Environment> {

    public static final String KEY = "bytechef.environment";

    @Override
    public Object key() {
        return KEY;
    }

    @Override
    public @Nullable Environment getValue() {
        return EnvironmentContext.fetchCurrentEnvironment();
    }

    @Override
    public void setValue(Environment value) {
        EnvironmentContext.set(value);
    }

    @Override
    public void setValue() {
        EnvironmentContext.clear();
    }
}
