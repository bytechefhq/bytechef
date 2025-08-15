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

/**
 * Defines an interface for contributing servlet filters that should be positioned after a specified filter in the web
 * security filter chain. Implementations of this interface provide the filter instance to be added and specify the
 * class of the filter it should follow.
 *
 * This interface is useful for customizations in the filter chain where the placement of a filter relative to others is
 * important to ensure the correct order of operations.
 *
 * @author Ivica Cardic
 */
public interface FilterAfterContributor {

    /**
     * Returns the {@link Filter} instance to be added to the web security filter chain. The filter is typically
     * configured to be placed after a specific filter class, ensuring proper order within the filter chain.
     *
     * @return the {@link Filter} instance to be added to the filter chain.
     */
    Filter getFilter();

    /**
     * Returns the class of the {@link Filter} that the contributed filter should be positioned after in the web
     * security filter chain.
     *
     * This method is used to ensure the correct order of filters within the filter chain, allowing the contributed
     * filter to follow a specific filter class.
     *
     * @return the class of the filter that this filter should be placed after in the filter chain
     */
    Class<? extends Filter> getAfterFilter();
}
