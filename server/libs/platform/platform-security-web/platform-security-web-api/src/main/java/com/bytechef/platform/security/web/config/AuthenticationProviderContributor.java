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

import org.springframework.security.authentication.AuthenticationProvider;

/**
 * Defines an interface for contributing custom {@link AuthenticationProvider} instances to the security configuration.
 * Implementations of this interface provide a specific {@link AuthenticationProvider} that is used for handling
 * authentication in a customized manner.
 *
 * This interface is useful for scenarios where multiple authentication mechanisms are required, allowing different
 * implementations to contribute distinct {@link AuthenticationProvider}s to the security framework.
 *
 * @author Ivica Cardic
 */
public interface AuthenticationProviderContributor {

    /**
     * Retrieves the {@link AuthenticationProvider} instance contributed by this implementation.
     *
     * @return the {@link AuthenticationProvider} instance used for authentication purposes.
     */
    AuthenticationProvider getAuthenticationProvider();
}
