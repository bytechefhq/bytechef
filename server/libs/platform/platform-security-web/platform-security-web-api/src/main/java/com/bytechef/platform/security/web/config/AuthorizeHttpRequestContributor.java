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
 * Defines an interface for contributing HTTP request paths to be designated as permit-all in the API security
 * configuration. Implementations of this interface provide specific paths that should be accessible without
 * authentication or authorization.
 *
 * This interface can be implemented for flexible customization of security rules in environments where certain API
 * endpoints or request patterns are expected to be publicly accessible.
 *
 * @author Ivica Cardic
 */
public interface AuthorizeHttpRequestContributor {

    /**
     * Provides a list of paths that should be matched as permit-all in the API security configuration.
     *
     * @return a list of string representations of paths to be allowed without authentication in the API.
     */
    default List<String> getApiPermitAllRequestMatcherPaths() {
        return List.of();
    }

    /**
     * Provides a list of paths that should be set to permit-all in the security configuration.
     *
     * @return a list of string representations of paths to be granted access without authentication.
     */
    default List<String> getPermitAllRequestMatcherPaths() {
        return List.of();
    }
}
