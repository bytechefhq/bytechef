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

package com.bytechef.platform.security.web.config;

import jakarta.servlet.Filter;
import org.springframework.security.authentication.AuthenticationManager;

/**
 * Defines an interface for contributing servlet filters that need to be positioned before a specific filter in the web
 * security filter chain. Implementations of this interface provide the filter instance to be added and specify the
 * class of the filter it should precede.
 *
 * This interface is particularly useful for customizing the web security filter chain by introducing filters in a
 * specific order to ensure the desired behavior and correct interaction between filters.
 *
 * @author Ivica Cardic
 */
public interface FilterBeforeContributor {

    /**
     * Returns a {@link Filter} instance configured using the provided {@link AuthenticationManager}. The returned
     * filter is typically added to a security filter chain, positioned before a designated filter.
     *
     * @param authenticationManager the {@link AuthenticationManager} instance used to configure the {@link Filter}.
     *                              This enables the implementation to use authentication-related logic when creating
     *                              the {@link Filter}.
     * @return the {@link Filter} instance to be added to the filter chain.
     */
    Filter getFilter(AuthenticationManager authenticationManager);

    /**
     * Returns the class of the {@link Filter} that the contributed filter should be placed before in the web security
     * filter chain.
     *
     * This method is used to ensure the correct order of filters within the filter chain, allowing the contributed
     * filter to precede a specific filter class.
     *
     * @return the class of the filter that this filter should be placed before in the filter chain
     */
    Class<? extends Filter> getBeforeFilter();
}
