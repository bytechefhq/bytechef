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

import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

/**
 * Represents an interface for contributing a custom {@link SecurityConfigurerAdapter} to a {@link SecurityBuilder}.
 * Implementations of this interface define security configurations that can be applied to the builder. This is useful
 * for modularizing security configuration logic and extending or customizing the behavior of the security framework by
 * injecting specific configurations into the security builders.
 */
@FunctionalInterface
public interface SecurityConfigurerContributor {

    /**
     * Retrieves a security configuration adapter for contributing custom configurations to a security builder. The
     * returned adapter can be used to configure various aspects of the security framework.
     *
     * @return an instance of a type extending {@link AbstractHttpConfigurer}, representing the security configuration
     *         adapter that can be applied to a security builder.
     */
    <T extends AbstractHttpConfigurer<T, B>, B extends HttpSecurityBuilder<B>> T getSecurityConfigurerAdapter();
}
