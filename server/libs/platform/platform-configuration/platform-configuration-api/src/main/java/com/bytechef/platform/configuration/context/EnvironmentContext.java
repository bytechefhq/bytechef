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
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread-bound current {@link Environment}, parallel to {@code TenantContext}. Set by callers around operations that
 * need the environment but cannot receive it as a parameter (e.g. Spring AI {@code VectorStore.add()} →
 * {@code EmbeddingModel.embed()}, where the embedding API key is environment-scoped). Defaults to
 * {@link Environment#PRODUCTION} when unset.
 *
 * @author Ivica Cardic
 */
public final class EnvironmentContext {

    private static final Logger log = LoggerFactory.getLogger(EnvironmentContext.class);

    private static final ThreadLocal<Environment> CONTEXT = new ThreadLocal<>();

    private EnvironmentContext() {
    }

    public static void set(Environment environment) {
        CONTEXT.set(environment);
    }

    public static void set(int ordinal) {
        Environment[] environments = Environment.values();

        if (ordinal < 0 || ordinal >= environments.length) {
            throw new IllegalArgumentException("Invalid environment ordinal: " + ordinal);
        }

        CONTEXT.set(environments[ordinal]);
    }

    public static Environment getCurrentEnvironment() {
        Environment environment = CONTEXT.get();

        if (environment == null) {
            log.debug("No environment set in EnvironmentContext; defaulting to {}", Environment.PRODUCTION);

            return Environment.PRODUCTION;
        }

        return environment;
    }

    /**
     * Returns the environment currently bound to the thread, or {@code null} when none is set. Unlike
     * {@link #getCurrentEnvironment()}, this does not fall back to {@link Environment#PRODUCTION}, so callers can
     * capture and later restore the exact prior binding (including "unset").
     */
    public static @Nullable Environment fetchCurrentEnvironment() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
