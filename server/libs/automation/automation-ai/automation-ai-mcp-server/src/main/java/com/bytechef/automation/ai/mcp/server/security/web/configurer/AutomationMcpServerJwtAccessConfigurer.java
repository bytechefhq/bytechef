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

package com.bytechef.automation.ai.mcp.server.security.web.configurer;

import com.bytechef.automation.ai.mcp.server.security.web.filter.AutomationMcpServerJwtAccessFilter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

/**
 * @author Ivica Cardic
 */
public class AutomationMcpServerJwtAccessConfigurer
    extends AbstractHttpConfigurer<AutomationMcpServerJwtAccessConfigurer, HttpSecurity> {

    private final AutomationMcpServerJwtAccessFilter automationMcpServerJwtAccessFilter;

    @SuppressFBWarnings("EI2")
    public AutomationMcpServerJwtAccessConfigurer(
        AutomationMcpServerJwtAccessFilter automationMcpServerJwtAccessFilter) {

        this.automationMcpServerJwtAccessFilter = automationMcpServerJwtAccessFilter;
    }

    @Override
    public void configure(HttpSecurity http) {
        http.addFilterBefore(automationMcpServerJwtAccessFilter, AuthorizationFilter.class);
    }
}
