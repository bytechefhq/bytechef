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

import java.util.List;

/**
 * Defines an interface for contributing path prefixes that should not be considered part of a Single Page Application
 * (SPA). Implementations of this interface provide flexibility in specifying non-SPA paths that are excluded from SPA
 * routing and processed separately, such as by backend services or static resource handlers.
 *
 * @author Ivica Cardic
 */
@FunctionalInterface
public interface SpaWebFilterContributor {

    /**
     * Provides a list of path prefixes that should not be considered part of a Single Page Application (SPA). These
     * paths are typically excluded from SPA routing and handled differently, such as by backend controllers or static
     * resource handling.
     *
     * @return a list of strings representing path prefixes that are excluded from SPA processing.
     */
    List<String> getNonSpaPathPrefixes();
}
