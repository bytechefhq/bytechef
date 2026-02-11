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

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

/**
 * Defines a customizer for OAuth2 login configuration. Implementations of this interface provide the necessary
 * configuration for integrating OAuth2 login (social login) into the security filter chain. When this bean is present,
 * it indicates that OAuth2 login is enabled and should be wired into the main API filter chain.
 *
 * @author Ivica Cardic
 */
@FunctionalInterface
public interface OAuth2LoginCustomizer {

    void customize(HttpSecurity http) throws Exception;
}
