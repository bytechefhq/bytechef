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

import com.bytechef.definition.BaseOptionsProperty;
import java.util.List;
import java.util.Optional;

/**
 * Represents a property that exposes a set of selectable {@link Option} values. The type parameter {@code T} denotes
 * the type of the values held by those options.
 *
 * @param <T> the type of the option values exposed by this property
 *
 * @author Ivica Cardic
 */
public interface OptionsProperty<T> extends BaseOptionsProperty<T, Option<T>> {

    /**
     * Retrieves the statically defined options available for this property.
     *
     * @return an {@link Optional} containing the list of options when they are defined; otherwise an empty
     *         {@link Optional}
     */
    Optional<List<? extends Option<T>>> getOptions();
}
