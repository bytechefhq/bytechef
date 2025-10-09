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

import java.util.List;

/**
 * The {@code PropertiesDataSource} interface defines a contract for providing a list of properties through a functional
 * approach. This interface supports retrieving dependent properties that a given implementation may rely on.
 *
 * @param <F> A type that extends {@link PropertiesDataSource.BasePropertiesFunction} and serves as the functional
 *            interface for defining how properties are retrieved.
 *
 * @author Ivica Cardic
 */
public interface PropertiesDataSource<F extends PropertiesDataSource.BasePropertiesFunction> {

    /**
     * Retrieves the functional interface implementation used for defining the retrieval of properties.
     *
     * @return an instance of the type {@code F}, which extends {@code PropertiesDataSource.BasePropertiesFunction},
     *         representing the functional method for getting properties.
     */
    F getProperties();

    /**
     * Retrieves a list of dependencies required for property lookup.
     *
     * @return a list of strings representing the names of dependent properties necessary for performing property
     *         lookup.
     */
    List<String> getPropertiesLookupDependsOn();

    /**
     * Marker interface representing a base functional contract for retrieving property definitions or operations within
     * the {@link PropertiesDataSource} context. This interface serves as a foundational functional abstraction,
     * enabling different implementations to define custom logic for retrieving or processing property-related data.
     * Specific functional interfaces that extend this base can define additional methods and semantics appropriate for
     * their use cases. It is typically used as a type constraint for defining generic functional behavior in property
     * data sources.
     */
    interface BasePropertiesFunction {
    }
}
