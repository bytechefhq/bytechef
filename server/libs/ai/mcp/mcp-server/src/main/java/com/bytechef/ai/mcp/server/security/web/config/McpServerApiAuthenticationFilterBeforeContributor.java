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

package com.bytechef.ai.mcp.server.security.web.config;

import com.bytechef.ai.mcp.server.security.web.filter.McpServerApiAuthenticationFilter;
import com.bytechef.platform.security.web.config.FilterBeforeContributor;
import jakarta.servlet.Filter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class McpServerApiAuthenticationFilterBeforeContributor implements FilterBeforeContributor {

    @Override
    public Filter getFilter(AuthenticationManager authenticationManager) {
        return new McpServerApiAuthenticationFilter(authenticationManager);
    }

    @Override
    public Class<? extends Filter> getBeforeFilter() {
        return BasicAuthenticationFilter.class;
    }
}
