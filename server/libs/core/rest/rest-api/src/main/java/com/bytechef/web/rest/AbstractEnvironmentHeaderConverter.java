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

package com.bytechef.web.rest;

import java.util.Locale;
import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.converter.Converter;

/**
 * Binds an {@code X-Environment} header to an API's generated environment enum the way the API-key filter reads it: any
 * letter case, surrounding whitespace ignored, and a blank value treated as absent so the controller falls back to its
 * default environment. An unknown name throws {@link IllegalArgumentException}, which Spring answers with a bad
 * request.
 *
 * @param <E> the API's generated environment enum
 *
 * @author Ivica Cardic
 */
public abstract class AbstractEnvironmentHeaderConverter<E extends Enum<E>> implements Converter<String, E> {

    private final Class<E> environmentType;

    protected AbstractEnvironmentHeaderConverter(Class<E> environmentType) {
        this.environmentType = environmentType;
    }

    @Override
    public @Nullable E convert(String source) {
        String name = source.trim();

        if (name.isEmpty()) {
            return null;
        }

        return Enum.valueOf(environmentType, name.toUpperCase(Locale.ROOT));
    }
}
