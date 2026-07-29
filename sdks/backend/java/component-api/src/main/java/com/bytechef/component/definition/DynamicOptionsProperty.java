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

package com.bytechef.component.definition;

import java.util.Optional;

/**
 * Represents a property whose selectable options can be resolved dynamically at runtime through an
 * {@link OptionsDataSource}, rather than being fixed at definition time. The type parameter {@code T} denotes the type
 * of the values held by the property's options.
 *
 * @param <T> the type of the option values exposed by this property
 *
 * @author Ivica Cardic
 */
public interface DynamicOptionsProperty<T> extends OptionsProperty<T> {

    /**
     * Retrieves the data source used to load this property's options dynamically.
     *
     * @return an {@link Optional} containing the {@link OptionsDataSource} when the property resolves its options
     *         dynamically; otherwise an empty {@link Optional}
     */
    Optional<OptionsDataSource<?>> getOptionsDataSource();
}
