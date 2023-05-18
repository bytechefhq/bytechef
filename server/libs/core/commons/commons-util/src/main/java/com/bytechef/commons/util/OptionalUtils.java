
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.commons.util;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Ivica Cardic
 */
public final class OptionalUtils {

    private OptionalUtils() {
    }

    public static <T> T get(Optional<T> optional) {
        Objects.requireNonNull(optional, "'optional' must not be null");

        return optional.orElseThrow(IllegalArgumentException::new);
    }

    public static <T> void ifPresent(Optional<T> optional, Consumer<? super T> action) {
        Objects.requireNonNull(optional, "'optional' must not be null");

        optional.ifPresent(action);
    }

    public static <T> boolean isPresent(Optional<T> optional) {
        Objects.requireNonNull(optional, "'optional' must not be null");

        return optional.isPresent();
    }

    public static <T> T orElse(Optional<T> optional, T elseObject) {
        Objects.requireNonNull(optional, "'optional' must not be null");

        return optional.orElse(elseObject);
    }

    public static <T> T orElseGet(Optional<T> optional, Supplier<? extends T> supplier) {
        Objects.requireNonNull(optional, "'optional' must not be null");

        return optional.orElseGet(supplier);
    }

    public static <T> void ifPresentOrElse(Optional<T> optional, Consumer<? super T> action, Runnable emptyAction) {
        Objects.requireNonNull(optional, "'optional' must not be null");

        optional.ifPresentOrElse(action, emptyAction);
    }

    public static <T, U> U map(Optional<T> optional, Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(optional, "'optional' must not be null");

        return optional
            .map(mapper)
            .orElseThrow(IllegalArgumentException::new);
    }

    public static <T, U> U mapOrElse(Optional<T> optional, Function<? super T, ? extends U> mapper, U other) {
        Objects.requireNonNull(optional, "'optional' must not be null");

        return optional
            .map(mapper)
            .map(u -> (U) u)
            .orElse(other);
    }
}
