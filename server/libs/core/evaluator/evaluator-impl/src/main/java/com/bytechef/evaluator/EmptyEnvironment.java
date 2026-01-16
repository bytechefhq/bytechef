/*
 * Copyright 2016-2020 the original author or authors.
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
 *
 * Modifications copyright (C) 2025 ByteChef
 */

package com.bytechef.evaluator;

import org.jspecify.annotations.Nullable;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/**
 * @author Arik Cohen
 * @since Mar, 06 2020
 */
class EmptyEnvironment implements Environment {

    @Override
    public boolean containsProperty(String key) {
        return false;
    }

    @Override
    public @Nullable String getProperty(String key) {
        return null;
    }

    @Override
    public @Nullable String getProperty(String key, String aDefaultValue) {
        return null;
    }

    @Override
    public <T> T getProperty(String key, Class<T> aTargetType) {
        return null;
    }

    @Override
    public <T> @Nullable T getProperty(String key, Class<T> aTargetType, T aDefaultValue) {
        return null;
    }

    @Override
    public @Nullable String getRequiredProperty(String key) throws IllegalStateException {
        return null;
    }

    @Override
    public <T> @Nullable T getRequiredProperty(String key, Class<T> aTargetType) throws IllegalStateException {
        return null;
    }

    @Override
    public @Nullable String resolvePlaceholders(String text) {
        return null;
    }

    @Override
    public @Nullable String resolveRequiredPlaceholders(String text) throws IllegalArgumentException {
        return null;
    }

    @Override
    public String[] getActiveProfiles() {
        return new String[0];
    }

    @Override
    public String[] getDefaultProfiles() {
        return new String[0];
    }

    @Override
    public boolean acceptsProfiles(String... aProfiles) {
        return false;
    }

    @Override
    public boolean acceptsProfiles(Profiles aProfiles) {
        return false;
    }

}
