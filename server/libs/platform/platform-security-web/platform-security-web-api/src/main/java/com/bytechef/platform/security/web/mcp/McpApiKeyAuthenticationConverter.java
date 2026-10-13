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

package com.bytechef.platform.security.web.mcp;

import com.bytechef.platform.configuration.service.EnvironmentService;
import jakarta.servlet.http.HttpServletRequest;

/**
 * @author Ivica Cardic
 */
public class McpApiKeyAuthenticationConverter extends AbstractMcpApiKeyAuthenticationConverter {

    public McpApiKeyAuthenticationConverter(EnvironmentService environmentService, String pathPrefix) {
        super(environmentService, pathPrefix);
    }

    @Override
    protected String extractServerSecretKey(HttpServletRequest request) {
        String servletPath = request.getServletPath();

        return servletPath.replace(pathPrefix, "")
            .replace("/mcp", "");
    }
}
