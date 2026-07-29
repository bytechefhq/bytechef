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

package com.bytechef.definition;

import java.util.List;
import java.util.Optional;

/**
 * Represents a property that offers a predefined set of selectable options, each pairing a label with an underlying
 * value of type {@code T}.
 *
 * @param <T> the type of the value held by each option
 * @param <I> the concrete {@link BaseOption} type describing the options
 * @author Ivica Cardic
 */
public interface BaseOptionsProperty<T, I extends BaseOption<T>> {

    /**
     * Returns the selectable options offered by this property.
     *
     * @return an {@link Optional} containing the list of options, or an empty {@link Optional} if none are defined
     */
    Optional<List<? extends I>> getOptions();
}
